package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.*
import javax.inject.Inject

class MediaFilesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore,
    private val dispatchers: CoroutineDispatchers
) {
    private var uploadContinuation = ContinuationWrapper<MediaModel>(T.MEDIA)
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
        }
    }

    suspend fun uploadFile(localUri: String): String {
        val mediaModel = fetchMedia(localUri)

        if (mediaModel == null) {
            WooLog.w(T.MEDIA, "MediaFilesRepository > null media")
            throw NullPointerException("null media")
        }

        val result = uploadContinuation.callAndWait {
            WooLog.d(T.MEDIA, "MediaFilesRepository > Dispatching request to upload $localUri")
            val payload = UploadMediaPayload(selectedSite.get(), mediaModel, true)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
        }

        return when (result) {
            is Cancellation -> throw result.exception
            is Success -> result.value.url
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
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
                )
                uploadContinuation.continueWithException(exception)
                producerScope.trySendBlocking(UploadResult.UploadFailure(exception))
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
                uploadContinuation.cancel()
            }
            event.completed -> {
                WooLog.i(T.MEDIA, "MediaFilesRepository > uploaded media ${event.media?.id}")
                uploadContinuation.continueWith(event.media)
                producerScope.trySendBlocking(
                    UploadResult.UploadSuccess(event.media)
                ).onFailure {
                    WooLog.w(
                        T.MEDIA,
                        "MediaFilesRepository > error delivering result, downstream collector may be cancelled"
                    )
                }
                producerScope.close()
            }
        }
    }

    class MediaUploadException(
        val media: MediaModel,
        val errorType: MediaStore.MediaErrorType,
        val errorMessage: String
    ) : Exception()

    sealed class UploadResult {
        data class UploadProgress(val progress: Int) : UploadResult()
        data class UploadSuccess(val media: MediaModel) : UploadResult()
        data class UploadFailure(val error: MediaUploadException) : UploadResult()
    }
}
