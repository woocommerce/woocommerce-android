package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import com.woocommerce.android.R
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadFailure
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadSuccess
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.*
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import java.io.File
import javax.inject.Inject

class MediaFilesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore,
    private val dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider
) {
    private lateinit var producerScope: ProducerScope<UploadResult>

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchMedia(localUri: String): MediaModel? {
        return withContext(dispatchers.io) {
            val mediaModel = ProductImagesUtils.mediaModelFromLocalUri(
                context,
                selectedSite.get().id,
                Uri.parse(localUri),
                mediaStore
            )

            if (mediaModel == null) {
                WooLog.w(T.MEDIA, "MediaFilesRepository > null media")
            }
            return@withContext mediaModel
        }
    }

    fun uploadMedia(localMediaModel: MediaModel, stripLocation: Boolean = true): Flow<UploadResult> {
        if (::producerScope.isInitialized) producerScope.cancel()

        return callbackFlow {
            producerScope = this

            WooLog.d(T.MEDIA, "MediaFilesRepository > Dispatching request to upload ${localMediaModel.filePath}")
            val payload = UploadMediaPayload(selectedSite.get(), localMediaModel, stripLocation)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))

            awaitClose {
                // Cancel upload if the collection was cancelled before completion
                if (!producerScope.isClosedForSend) {
                    val payload = CancelMediaPayload(selectedSite.get(), localMediaModel, true)
                    dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
                }
            }
        }.onEach {
            if (it is UploadSuccess) {
                // Remove local file if it's in cache directory
                if (localMediaModel.filePath.contains(context.cacheDir.absolutePath)) {
                    File(localMediaModel.filePath).delete()
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
                            media = MediaModel(),
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

    @Suppress("LongMethod")
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (!::producerScope.isInitialized) return
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
                val channelResult = if (event.media?.url != null) {
                    WooLog.i(T.MEDIA, "MediaFilesRepository > uploaded media ${event.media?.id}")
                    producerScope.trySendBlocking(
                        UploadSuccess(event.media)
                    )
                } else {
                    WooLog.w(
                        T.MEDIA,
                        "MediaFilesRepository > error uploading media ${event.media?.id}, null url"
                    )

                    producerScope.trySendBlocking(
                        UploadFailure(
                            error = MediaUploadException(
                                event.media,
                                GENERIC_ERROR,
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

    class MediaUploadException(
        val media: MediaModel,
        val errorType: MediaStore.MediaErrorType,
        val errorMessage: String
    ) : Exception()

    sealed class UploadResult {
        data class UploadProgress(val progress: Float) : UploadResult()
        data class UploadSuccess(val media: MediaModel) : UploadResult()
        data class UploadFailure(val error: MediaUploadException) : UploadResult()
    }
}
