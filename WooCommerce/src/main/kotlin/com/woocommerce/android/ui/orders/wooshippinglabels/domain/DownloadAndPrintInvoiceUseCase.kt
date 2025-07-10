package com.woocommerce.android.ui.orders.wooshippinglabels.domain

import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.FileDownloader
import java.io.File
import javax.inject.Inject

/**
 * A use case that handles downloading an invoice from a URL and saving it to a temporary file.
 */
class DownloadAndPrintInvoiceUseCase @Inject constructor(
    private val fileUtils: FileUtils,
    private val fileDownloader: FileDownloader
) {
    /**
     * Downloads an invoice file from the given URL.
     *
     * @param invoiceUrl The URL of the invoice to download.
     * @param storageDir The directory where the temporary file will be created.
     * @return A [Result] containing the downloaded [File] on success, or an exception on failure.
     */
    suspend operator fun invoke(invoiceUrl: String, storageDir: File): Result<File> {
        val tempFile = fileUtils.createTempTimeStampedFile(
            storageDir = storageDir,
            prefix = "PDF",
            fileExtension = "pdf"
        ) ?: return Result.failure(Exception("Failed to create temp file for invoice."))

        return if (fileDownloader.downloadFile(invoiceUrl, tempFile)) {
            Result.success(tempFile)
        } else {
            Result.failure(Exception("Failed to download invoice file."))
        }
    }
}
