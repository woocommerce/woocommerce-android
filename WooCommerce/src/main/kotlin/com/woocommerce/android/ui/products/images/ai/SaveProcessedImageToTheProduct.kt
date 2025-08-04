package com.woocommerce.android.ui.products.images.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class SaveProcessedImageToTheProduct @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileUtils: FileUtils,
    private val mediaFileUploadHandler: MediaFileUploadHandler
) {
    private suspend fun createTempFileUri(processedBitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val tempFile = fileUtils.createTempTimeStampedFile(
            storageDir = context.cacheDir,
            prefix = "processed_image",
            fileExtension = "jpg"
        )

        requireNotNull(tempFile) { "Failed to create temporary file" }

        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()

        val writtenFile = fileUtils.writeContentToFile(tempFile, byteArray)
        requireNotNull(writtenFile) { "Failed to write bitmap to file" }

        Uri.fromFile(tempFile).toString()
    }

    suspend operator fun invoke(processedBitmap: Bitmap, remoteProductId: Long) = withContext(Dispatchers.IO) {
        val fileUri = createTempFileUri(processedBitmap)
        mediaFileUploadHandler.enqueueUpload(remoteProductId, listOf(fileUri))
    }

    companion object {
        private const val JPEG_QUALITY = 99
    }
}
