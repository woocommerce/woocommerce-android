package com.woocommerce.android.media

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.UTILS
import java.io.File
import javax.inject.Inject

class FileUtils @Inject constructor() {
    /**
     * Creates a temp file with the filename having the format: "{[prefix]}_{yyyyMMdd_HHmmss}.{[fileExtension]}"
     * in the specified [storageDir]
     */
    fun createTempTimeStampedFile(
        storageDir: File,
        prefix: String,
        fileExtension: String
    ): File? {
        return try {
            val fileName = "${prefix}_${System.currentTimeMillis()}"
            File.createTempFile(fileName, ".$fileExtension", storageDir)
        } catch (e: Exception) {
            WooLog.e(UTILS, "Unable to create a temp file", e)
            null
        }
    }

    /**
     * Writes content to the file.
     * If the file already exists, its content will be overwritten
     */
    fun writeContentToFile(
        file: File,
        content: ByteArray
    ): File? {
        return try {
            file.outputStream().use {
                it.write(content)
            }
            file
        } catch (e: Exception) {
            WooLog.e(UTILS, "Unable to write to file $file", e)
            null
        }
    }
}
