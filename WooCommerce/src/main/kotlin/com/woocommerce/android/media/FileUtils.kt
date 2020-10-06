package com.woocommerce.android.media

import android.util.Base64
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.UTILS
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    /**
     * Creates a temp file with the given [fileExtension]
     */
    fun createTempFile(
        storageDir: File,
        fileExtension: String = "pdf"
    ): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "PDF_" + timeStamp + "_"
            File.createTempFile(imageFileName, ".$fileExtension", storageDir)
        } catch (e: Exception) {
            WooLog.e(UTILS, "Unable to create a temp file", e)
            null
        }
    }

    /**
     * writes the incoming [stringToWrite] into the [tempFile]
     */
    fun writeToTempFile(
        tempFile: File,
        stringToWrite: String
    ): File? {
        return try {
            if (tempFile.exists()) tempFile.delete()

            val out = FileOutputStream(tempFile)
            val pdfAsBytes = Base64.decode(stringToWrite, 0)
            out.write(pdfAsBytes)
            out.flush()
            out.close()

            tempFile
        } catch (e: Exception) {
            WooLog.e(UTILS, "Unable to write to temp file", e)
            null
        }
    }
}
