package com.woocommerce.android.ui.payments.receipt

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.woocommerce.android.R
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FileDownloader
import java.io.File
import javax.inject.Inject

class PaymentReceiptShare @Inject constructor(
    private val fileUtils: FileUtils,
    private val fileDownloader: FileDownloader,
    private val context: Application,
    private val selectedSite: SelectedSite,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(receiptUrl: String, orderNumber: Long): ReceiptShareResult {
        val receiptFile = fileUtils.createTempTimeStampedFile(
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir,
            prefix = "receipt_$orderNumber",
            fileExtension = "html"
        )
        return when {
            receiptFile == null -> ReceiptShareResult.Error.FileCreation
            !fileDownloader.downloadFile(receiptUrl, receiptFile) -> ReceiptShareResult.Error.FileDownload
            else -> tryShare(receiptFile)
        }
    }

    private fun tryShare(receiptFile: File): ReceiptShareResult {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            receiptFile
        )

        val text = context.getString(
            R.string.card_reader_payment_receipt_email_subject,
            selectedSite.get().name.orEmpty()
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
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
