package com.woocommerce.android.media

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.util.Patterns
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadFailure
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadSuccess
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.mediapicker.MediaPickerUtils
import org.wordpress.android.util.MediaUtils
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.net.URL
import javax.inject.Inject

class MediaFilesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore,
    private val dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider,
    private val mediaPickerUtils: MediaPickerUtils
) {
    suspend fun fetchWordPressMedia(mediaId: Long): Result<MediaModel> {
        val result = dispatcher.dispatchAndAwait<MediaPayload, OnMediaChanged>(
            action = MediaActionBuilder.newFetchMediaAction(
                MediaPayload(
                    selectedSite.get(),
                    MediaModel(selectedSite.get().localId().value, mediaId)
                )
            )
        )

        return if (result.isError) {
            Result.failure(OnChangedException(result.error))
        } else {
            Result.success(result.mediaList.first())
        }
    }

    suspend fun fetchMedia(localUri: String): MediaModel? {
        return withContext(dispatchers.io) {
            val mediaModel = FileUploadUtils.mediaModelFromLocalUri(
                context,
                selectedSite.get().id,
                Uri.parse(localUri),
                mediaStore,
                mediaPickerUtils
            )

            if (mediaModel == null) {
                WooLog.w(T.MEDIA, "MediaFilesRepository > null media")
            }
            return@withContext mediaModel
        }
    }

    suspend fun getImageDimensions(uri: String): ImageDimensions {
        return withContext(dispatchers.io) {
            try {
                val options = Options().apply { inJustDecodeBounds = true }
                if (Patterns.WEB_URL.matcher(uri).matches()) {
                    BitmapFactory.decodeStream(URL(uri).openConnection().getInputStream(), null, options)
                } else {
                    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
                    val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
                    BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
                    parcelFileDescriptor.close()
                }
                return@withContext ImageDimensions(options.outWidth, options.outHeight)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return@withContext ImageDimensions(width = 0, height = 0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun uploadMedia(localMediaModel: MediaModel, stripLocation: Boolean = true): Flow<UploadResult> {
        return callbackFlow {
            WooLog.d(T.MEDIA, "MediaFilesRepository > Dispatching request to upload ${localMediaModel.filePath}")
            val listener = OnMediaUploadListener(this)
            dispatcher.register(listener)
            val uploadPayload = MediaStore.UploadMediaPayload(
                selectedSite.get(),
                localMediaModel,
                if (MediaUtils.isValidImage(localMediaModel.filePath)) stripLocation else false
            )
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(uploadPayload))

            awaitClose {
                dispatcher.unregister(listener)
                // Cancel upload if the collection was cancelled before completion
                if (!isClosedForSend) {
                    val cancelPayload = MediaStore.CancelMediaPayload(selectedSite.get(), localMediaModel, true)
                    dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload))
                }
            }
        }.onEach {
            if (it is UploadSuccess) {
                // Remove local file if it's in cache directory
                val filePath = localMediaModel.filePath
                if (filePath != null && filePath.contains(context.cacheDir.absolutePath)) {
                    File(filePath).delete()
                }
            }
        }
    }

    fun uploadFile(localUri: String): Flow<UploadResult> {
        return flow {
            val mediaModel = fetchMedia(localUri)

            if (mediaModel == null) {
                WooLog.w(T.MEDIA, "MediaFilesRepository > null media")
                emit(
                    UploadFailure(
                        error = MediaUploadException(
                            errorMessage = "Media couldn't be found",
                            errorType = MediaStore.MediaErrorType.NULL_MEDIA_ARG
                        )
                    )
                )
                return@flow
            }

            emitAll(uploadMedia(mediaModel))
        }
    }

    private inner class OnMediaUploadListener(private val producerScope: ProducerScope<UploadResult>) {
        @Suppress("LongMethod")
        @SuppressWarnings("unused")
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        fun onMediaUploaded(event: OnMediaUploaded) {
            when {
                event.isError -> {
                    WooLog.w(
                        T.MEDIA,
                        "MediaFilesRepository > error uploading media: ${event.error.type}, ${event.error.message}"
                    )
                    val exception = MediaUploadException(
                        event.media,
                        event.error.type,
                        event.error.message
                            ?: resourceProvider.getString(R.string.product_image_service_error_uploading)
                    )
                    producerScope.trySendBlocking(UploadFailure(exception))
                        .onFailure {
                            WooLog.w(
                                T.MEDIA,
                                "MediaFilesRepository > error delivering result, downstream collector may be cancelled"
                            )
                        }
                    producerScope.close()
                }

                event.canceled -> {
                    WooLog.d(T.MEDIA, "MediaFilesRepository > upload media cancelled")
                }

                event.completed -> {
                    val media = event.media
                    val channelResult = if (media != null && media.url.isNotNullOrEmpty()) {
                        WooLog.i(T.MEDIA, "MediaFilesRepository > uploaded media ${media.id}")
                        producerScope.trySendBlocking(
                            UploadSuccess(media)
                        )
                    } else {
                        WooLog.w(
                            T.MEDIA,
                            "MediaFilesRepository > error uploading media [null media or blank url ${media?.id}]"
                        )

                        producerScope.trySendBlocking(
                            UploadFailure(
                                error = MediaUploadException(
                                    media,
                                    MediaStore.MediaErrorType.GENERIC_ERROR,
                                    resourceProvider.getString(R.string.product_image_service_error_uploading)
                                )
                            )
                        )
                    }
                    channelResult.onFailure {
                        WooLog.w(
                            T.MEDIA,
                            "MediaFilesRepository > error delivering result, downstream collector may be cancelled"
                        )
                    }
                    producerScope.close()
                }

                else -> {
                    producerScope.trySend(
                        UploadResult.UploadProgress(event.progress)
                    ).onFailure {
                        WooLog.w(
                            T.MEDIA,
                            "MediaFilesRepository > error delivering result, downstream collector may be cancelled"
                        )
                    }
                }
            }
        }
    }

    class MediaUploadException(
        val media: MediaModel? = null,
        val errorType: MediaStore.MediaErrorType,
        val errorMessage: String
    ) : Exception()

    sealed class UploadResult {
        data class UploadProgress(val progress: Float) : UploadResult()
        data class UploadSuccess(val media: MediaModel) : UploadResult()
        data class UploadFailure(val error: MediaUploadException) : UploadResult()
    }

    data class ImageDimensions(val width: Int, val height: Int)
}
