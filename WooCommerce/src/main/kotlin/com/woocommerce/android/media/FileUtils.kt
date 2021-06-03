package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.content.FileProvider
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.UTILS
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    /**
     * Creates a temp PDF file
     */
    fun createTempPDFFile(
        storageDir: File
    ): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PDF_" + timeStamp + "_"
            File.createTempFile(fileName, ".pdf", storageDir)
        } catch (e: Exception) {
            WooLog.e(UTILS, "Unable to create a temp file", e)
            null
        }
    }

    fun Context.previewPDFFile(file: File) {
        val pdfUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", file
        )

        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.setDataAndType(pdfUri, "application/pdf")
        sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(sendIntent)
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
