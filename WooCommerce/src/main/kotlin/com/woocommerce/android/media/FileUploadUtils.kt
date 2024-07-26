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

object FileUploadUtils {
    private const val OPTIMIZE_IMAGE_MAX_SIZE = 3000
    private const val OPTIMIZE_IMAGE_QUALITY = 85

    @Suppress("ReturnCount")
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
                val path = if (MediaUtils.isValidImage(filePath)) {
                    // optimize the image if the setting is enabled
                    getOptimizedImagePath(context, filePath)
                } else {
                    filePath
                }

                val mimeType = getMimeType(context, localUri, fetchedUri) ?: return null

                val file = File(path)
                if (file.exists()) {
                    return createMediaModel(mediaStore, fetchedUri, path, localSiteId, mimeType)
                } else {
                    WooLog.w(T.MEDIA, "mediaModelFromLocalUri > file does not exist, $path")
                }
            } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to get path from uri, $fetchedUri")
        } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > fetched media path is null or empty")

        return null
    }

    private fun getMimeType(context: Context, originalUri: Uri, fetchedUri: Uri): String? {
        return originalUri.takeIf { it.scheme == "content" }
            ?.let { context.contentResolver.getType(originalUri) }
            ?: UrlUtils.getUrlMimeType(fetchedUri.toString())
            ?: FluxCMediaUtils.getExtension(fetchedUri.path).let { extension ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
    }

    @Suppress("MagicNumber")
    private fun createMediaModel(
        mediaStore: MediaStore,
        fetchedUri: Uri,
        path: String,
        localSiteId: Int,
        mimeType: String
    ): MediaModel? {
        var filename = FluxCMediaUtils.getFileName(fetchedUri.path) ?: ""
        val fileExtension: String = filename
            .substringAfterLast(delimiter = ".", missingDelimiterValue = "")
            .ifBlank {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.also {
                    // Filename doesn't have an extension in this case
                    filename += ".$it"
                }
            }
            ?: run {
                // If file extension is null, upload won't work on wordpress.com
                WooLog.w(T.MEDIA, "We couldn't identify the file's extension")
                return null
            }

        val media = MediaModel(
            localSiteId,
            DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000),
            filename,
            path,
            fileExtension,
            mimeType,
            filename,
            null
        )
        val instantiatedMedia = mediaStore.instantiateMediaModel(media)
        return if (instantiatedMedia != null) {
            instantiatedMedia
        } else {
            WooLog.w(T.MEDIA, "We couldn't instantiate the media")
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getOptimizedImagePath(
        context: Context,
        filePath: String
    ) = if (AppPrefs.getImageOptimizationEnabled()) {
        getOptimizedImageUri(context, filePath)?.let { uri ->
            val realPath = MediaUtils.getRealPathFromURI(context, uri)
            if (realPath == null) {
                WooLog.w(T.MEDIA, "Finding real path for uri $uri failed")
            }
            realPath
        }?.let { optimizedPath ->
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
        return try {
            MediaUtils.downloadExternalMedia(context.applicationContext, mediaUri)
        } catch (e: IllegalStateException) {
            WooLog.e(T.MEDIA, "Can't download the file at: $mediaUri", e)
            null
        } catch (e: SecurityException) {
            WooLog.e(T.MEDIA, "Can't download the file at: $mediaUri", e)
            null
        }
    }

    /**
     * Resize and compress the passed image
     */
    @Suppress("TooGenericExceptionCaught")
    private fun getOptimizedImageUri(context: Context, path: String): Uri? {
        try {
            ImageUtils.optimizeImage(context, path, OPTIMIZE_IMAGE_MAX_SIZE, OPTIMIZE_IMAGE_QUALITY)?.let { optPath ->
                return Uri.parse(optPath)
            }
            WooLog.w(T.MEDIA, "getOptimizedMedia > Optimized picture was null!")
        } catch (e: Exception) {
            WooLog.e(T.MEDIA, "mediaModelFromLocalUri > failed to optimize image", e)
        }
        return null
    }
}
