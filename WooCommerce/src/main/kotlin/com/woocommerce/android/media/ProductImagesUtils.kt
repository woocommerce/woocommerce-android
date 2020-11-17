package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UrlUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProductImagesUtils {
    private const val OPTIMIZE_IMAGE_MAX_SIZE = 3000
    private const val OPTIMIZE_IMAGE_QUALITY = 85

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

        var path = MediaUtils.getRealPathFromURI(context, fetchedUri)
        if (path == null) {
            WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to get path from uri, $fetchedUri")
            return null
        }

        // optimize the image if the setting is enabled
        if (AppPrefs.getImageOptimizationEnabled()) {
            getOptimizedImagePath(context, path)?.let {
                path = it
            } ?: WooLog.w(T.MEDIA, "mediaModelFromLocalUri > failed to optimize image")
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
        } catch (e: SecurityException) {
            WooLog.e(T.MEDIA, "Can't download the image at: $mediaUri", e)
            null
        }
    }

    /**
     * Creates a temporary file for storing captured photos
     */
    private fun createCaptureImageFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                    "JPEG_${timeStamp}_",
                    ".jpg",
                    storageDir
            )
        } catch (ex: IOException) {
            WooLog.e(T.MEDIA, ex)
            null
        }
    }

    private fun hasCamera(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    /**
     * Create an intent for capturing a device photo
     */
    fun createCaptureImageIntent(context: Context): Intent? {
        if (!hasCamera(context)) {
            return null
        }

        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure that there's a camera activity to handle the intent
            intent.resolveActivity(context.packageManager)?.also {
                createCaptureImageFile(context)?.also { file ->
                    // capturedPhotoPath = file.absolutePath
                    val authority = context.applicationContext.packageName + ".provider"
                    val imageUri = FileProvider.getUriForFile(
                            context,
                            authority,
                            file
                    )
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri)
                    return intent
                }
            }
        }
        return null
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

    private fun getOptimizedImagePath(context: Context, path: String): String? {
        getOptimizedImageUri(context, path)?.let { optUri ->
            MediaUtils.getRealPathFromURI(context, optUri)?.let {
                return it
            }
        }
        return null
    }
}
