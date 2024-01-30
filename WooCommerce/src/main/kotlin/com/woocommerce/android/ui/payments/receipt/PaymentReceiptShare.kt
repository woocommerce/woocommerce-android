package com.woocommerce.android.ui.payments.receipt

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.FileDownloader
import javax.inject.Inject

class PaymentReceiptShare @Inject constructor(
    private val fileUtils: FileUtils,
    private val fileDownloader: FileDownloader,
    private val context: Application,
) {
    suspend operator fun invoke(receiptUrl: String): ReceiptShareResult {
        val receiptFile = fileUtils.createTempTimeStampedFile(
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir,
            prefix = "receipt",
            fileExtension = "html"
        ) ?: return ReceiptShareResult.Error.FileCreation
        if (!fileDownloader.downloadFile(receiptUrl, receiptFile)) {
            return ReceiptShareResult.Error.FileDownload
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            receiptFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        return try {
            context.startActivity(
                Intent.createChooser(intent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            ReceiptShareResult.Success
        } catch (e: Exception) {
            ReceiptShareResult.Error.Sharing(e)
        }
    }

    sealed class ReceiptShareResult {
        object Success : ReceiptShareResult()
        sealed class Error : ReceiptShareResult() {
            data class Sharing(val exception: Exception) : Error()
            object FileCreation : Error()
            object FileDownload : Error()
        }
    }
}
