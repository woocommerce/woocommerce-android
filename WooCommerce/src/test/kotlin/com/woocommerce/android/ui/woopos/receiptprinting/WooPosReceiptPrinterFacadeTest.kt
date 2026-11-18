package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosReceiptPrinterFacadeTest {
    @get:Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val connectionStatus = MutableStateFlow<ReceiptPrinterConnectionStatus>(
        ReceiptPrinterConnectionStatus.Connected(DEVICE)
    )
    private val receiptPrinterManager: ReceiptPrinterManager = mock {
        on { this.connectionStatus } doReturn connectionStatus
        on { discover() } doReturn flowOf(ReceiptPrinterDiscoveryEvent.PrinterFound(DEVICE))
        on { connect(DEVICE) } doReturn ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound)
        on { printReceipt(RECEIPT_TEXT) } doReturn ReceiptPrinterResult.Success
    }

    private val sut = WooPosReceiptPrinterFacade(receiptPrinterManager)

    @Test
    fun `when reading connection status, then it mirrors the manager`() {
        assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Connected(DEVICE))
    }

    @Test
    fun `when discover is collected, then the manager events are emitted`() = runTest {
        // WHEN
        val events = sut.discover().toList()

        // THEN
        assertThat(events).containsExactly(ReceiptPrinterDiscoveryEvent.PrinterFound(DEVICE))
    }

    @Test
    fun `when connect is called, then the manager result is returned`() = runTest {
        // WHEN
        val result = sut.connect(DEVICE)

        // THEN
        assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
    }

    @Test
    fun `when disconnect is called, then the manager disconnects`() = runTest {
        // WHEN
        sut.disconnect()

        // THEN
        verify(receiptPrinterManager).disconnect()
    }

    @Test
    fun `when printReceipt is called, then the manager result is returned`() = runTest {
        // WHEN
        val result = sut.printReceipt(RECEIPT_TEXT)

        // THEN
        assertThat(result).isEqualTo(ReceiptPrinterResult.Success)
    }

    private companion object {
        val DEVICE = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
        const val RECEIPT_TEXT = "Total: 10.00"
    }
}
