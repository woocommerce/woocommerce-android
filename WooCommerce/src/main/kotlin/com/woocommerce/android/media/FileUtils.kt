package com.woocommerce.android.media

import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    /**
     * Creates a temp pdf file to store the shipping label pdf
     */
    fun createTempPdfFile(storageDir: File): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "PDF_" + timeStamp + "_"
        return File.createTempFile(imageFileName, ".pdf", storageDir)
    }

    /**
     * writes the preview string for a shipping label into the [tempFile]
     */
    fun writePdfToTempFile(tempFile: File, stringToWrite: String): File? {
        return try {
            if (tempFile.exists()) tempFile.delete()

            val out = FileOutputStream(tempFile)
            val pdfAsBytes = Base64.decode(stringToWrite, 0)
            out.write(pdfAsBytes)
            out.flush()
            out.close()

            tempFile
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }
}
