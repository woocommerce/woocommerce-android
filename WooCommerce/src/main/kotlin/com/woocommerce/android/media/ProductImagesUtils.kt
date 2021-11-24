package com.woocommerce.android.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.mediapicker.MediaPickerUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UrlUtils
import java.io.File
import org.wordpress.android.fluxc.utils.MediaUtils as FluxCMediaUtils

object ProductImagesUtils {
    private const val OPTIMIZE_IMAGE_MAX_SIZE = 3000
    private const val OPTIMIZE_IMAGE_QUALITY = 85

    fun mediaModelFromLocalUri(
        context: Context,
        localSiteId: Int,
        localUri: Uri,
        mediaStore: MediaStore,
        mediaPickerUtils: MediaPickerUtils
    ): MediaModel? {
        // "fetch" the media - necessary to support choosing from Downloads, Google Photos, etc.
        fetchMedia(context, localUri)?.let { fetchedUri ->
            mediaPickerUtils.getFilePath(fetchedUri)?.let { filePath ->
                // optimize the image if the setting is enabled
                val path = getOptimizedPath(context, filePath)

                val file = File(path)
                if (file.exists()) {
                    return createMediaModel(mediaStore, fetchedUri, path, localSiteId)
                } else {
                    WooLog.w(T.MEDIA, "mediaModelFromLocalUri > file does not exist, $path")
                }
            } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to get path from uri, $fetchedUri")
        } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > fetched media path is null or empty")

        return null
    }

    private fun createMediaModel(
        mediaStore: MediaStore,
        fetchedUri: Uri,
        path: String,
        localSiteId: Int
    ): MediaModel? {
        val media = mediaStore.instantiateMediaModel()
        var filename = FluxCMediaUtils.getFileName(fetchedUri.path)
        var fileExtension: String? = FluxCMediaUtils.getExtension(fetchedUri.path)

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
        media.filePath = path
        media.localSiteId = localSiteId
        media.fileExtension = fileExtension
        media.mimeType = mimeType
        media.uploadDate = DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000)

        return media
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getOptimizedPath(
        context: Context,
        filePath: String
    ) = if (AppPrefs.getImageOptimizationEnabled()) {
        getOptimizedImagePath(context, filePath)?.let { optimizedPath ->
            // Delete original file if it's in the cache directly
            if (filePath.contains(context.cacheDir.absolutePath)) File(filePath).delete()
            // Return optimized path
            optimizedPath
        } ?: filePath
    } else {
        filePath
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
            MediaUtils.downloadExternalMedia(context.applicationContext, mediaUri)
        } catch (e: IllegalStateException) {
            WooLog.e(T.MEDIA, "Can't download the image at: $mediaUri", e)
            null
        } catch (e: SecurityException) {
            WooLog.e(T.MEDIA, "Can't download the image at: $mediaUri", e)
            null
        }
    }

    /**
     * Resize and compress the passed image
     */
    private fun getOptimizedImageUri(context: Context, path: String): Uri? {
        ImageUtils.optimizeImage(context, path, OPTIMIZE_IMAGE_MAX_SIZE, OPTIMIZE_IMAGE_QUALITY)?.let { optPath ->
            return Uri.parse(optPath)
        }

        WooLog.w(T.MEDIA, "getOptimizedMedia > Optimized picture was null!")
        return null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getOptimizedImagePath(context: Context, path: String): String? {
        try {
            getOptimizedImageUri(context, path)?.let { optUri ->
                MediaUtils.getRealPathFromURI(context, optUri)?.let {
                    return it
                }
            } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to optimize image")
        } catch (e: Exception) {
            WooLog.e(T.MEDIA, "mediaModelFromLocalUri > failed to optimize image", e)
        }

        return null
    }
}
