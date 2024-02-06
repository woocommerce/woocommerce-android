package com.woocommerce.android.ui.payments.receipt

import android.app.Application
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class PaymentReceiptShareTest : BaseUnitTest() {
    private val fileUtils: FileUtils = mock()
    private val fileDownloader: FileDownloader = mock()

    private val file: File = mock()
    private val context: Application = mock {
        on { getExternalFilesDir(anyOrNull()) }.thenReturn(file)
    }

    private val sut = PaymentReceiptShare(
        fileUtils = fileUtils,
        fileDownloader = fileDownloader,
        context = context,
    )

    @Test
    fun `given file not created, when invoke, then FileCreation error returned`() = testBlocking {
        // GIVEN
        whenever(
            fileUtils.createTempTimeStampedFile(
                storageDir = anyOrNull(),
                prefix = eq("receipt_999"),
                fileExtension = eq("html"),
            )
        ).thenReturn(null)

        // WHEN
        val result = sut("receiptUrl", 999L)

        // THEN
        assertThat(result).isInstanceOf(PaymentReceiptShare.ReceiptShareResult.Error.FileCreation::class.java)
    }

    @Test
    fun `given file created but not downloaded, when invoke, then FileDownload error returned`() = testBlocking {
        // GIVEN
        whenever(
            fileUtils.createTempTimeStampedFile(
                storageDir = anyOrNull(),
                prefix = eq("receipt_999"),
                fileExtension = eq("html"),
            )
        ).thenReturn(file)
        whenever(fileDownloader.downloadFile(eq("receiptUrl"), eq(file))).thenReturn(false)

        // WHEN
        val result = sut("receiptUrl", 999L)

        // THEN
        assertThat(result).isInstanceOf(PaymentReceiptShare.ReceiptShareResult.Error.FileDownload::class.java)
    }
}
