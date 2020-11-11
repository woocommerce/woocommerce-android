package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaFilesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val mediaStore: MediaStore
) {
    private var uploadContinuation: CancellableContinuation<String>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun uploadFile(localUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            val mediaModel = ProductImagesUtils.mediaModelFromLocalUri(
                context,
                selectedSite.get().id,
                localUri,
                mediaStore
            )
            if (mediaModel == null) {
                WooLog.w(T.MEDIA, "MediaFilesRepository > null media")
                continuation.resumeWithException(NullPointerException("null media"))
                return@suspendCancellableCoroutine
            }
            uploadContinuation = continuation
            WooLog.d(T.MEDIA, "MediaFilesRepository > Dispatching request to upload $localUri")
            val payload = UploadMediaPayload(selectedSite.get(), mediaModel, true)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
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
                uploadContinuation?.resumeWithException(Exception("${event.error.type}, ${event.error.message}"))
            }
            event.canceled -> {
                WooLog.d(T.MEDIA, "MediaFilesRepository > upload media cancelled")
                uploadContinuation?.cancel()
            }
            event.completed -> {
                WooLog.i(T.MEDIA, "MediaFilesRepository > uploaded media ${event.media?.id}")
                uploadContinuation?.resume(event.media.url)
            }
        }
    }
}
