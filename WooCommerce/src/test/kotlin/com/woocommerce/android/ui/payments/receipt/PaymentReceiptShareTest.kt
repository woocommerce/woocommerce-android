package com.woocommerce.android.ui.payments.receipt

import android.app.Application
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class PaymentReceiptShareTest : BaseUnitTest() {
    private val fileUtils: FileUtils = mock()
    private val fileDownloader: FileDownloader = mock()
    private val context: Application = mock()

    private val sut = PaymentReceiptShare(
        fileUtils = fileUtils,
        fileDownloader = fileDownloader,
        context = context,
    )

    @Test
    fun `test receipt share`() = testBlocking {
        sut.invoke("receiptUrl", 123L)
    }
}
