package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UrlUtils
import java.io.File

object MediaUploadUtils {
    fun mediaModelFromLocalUri(
        context: Context,
        localSiteId: Int,
        localUri: Uri,
        mediaStore: MediaStore
    ): MediaModel? {
        // "fetch" the media - necessary to support choosing from Downloads, Google Photos, etc.
        val fetchedUri = fetchMedia(context, localUri)
        if (fetchedUri == null) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > fetched media path is null or empty")
            return null
        }

        val path = MediaUtils.getRealPathFromURI(context, fetchedUri)
        if (path == null) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to get path from uri, $fetchedUri")
            return null
        }

        val file = File(path)
        if (!file.exists()) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > file does not exist, $path")
            return null
        }

        val media = mediaStore.instantiateMediaModel()
        var filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(fetchedUri.path)
        var fileExtension: String? = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(fetchedUri.path)

        var mimeType = UrlUtils.getUrlMimeType(fetchedUri.toString())
        if (mimeType == null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
            if (mimeType == null) {
                // Default to image jpeg
                mimeType = "image/jpeg"
            }
        }

        // If file extension is null, upload won't work on wordpress.com
        if (fileExtension.isNullOrBlank()) {
            fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            filename += "." + fileExtension!!
        }

        media.fileName = filename
        media.title = filename
        media.filePath = fetchedUri.path
        media.localSiteId = localSiteId
        media.fileExtension = fileExtension
        media.mimeType = mimeType
        media.uploadDate = DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000)

        return media
    }

    /**
     * Downloads the {@code mediaUri} and returns the {@link Uri} for the downloaded file
     * <p>
     * If the {@code mediaUri} is already in the the local store, no download will be done and the given
     * {@code mediaUri} will be returned instead. This may return null if the download fails.
     * <p>
     * The current thread is blocked until the download is finished.
     *
     * @return A local {@link Uri} or null if the download failed
     */
    private fun fetchMedia(context: Context, mediaUri: Uri): Uri? {
        if (MediaUtils.isInMediaStore(mediaUri)) {
            return mediaUri
        }

        return try {
            MediaUtils.downloadExternalMedia(context, mediaUri)
        } catch (e: IllegalStateException) {
            WooLog.e(T.MEDIA, "Can't download the image at: $mediaUri", e)
            null
        }
    }
}
