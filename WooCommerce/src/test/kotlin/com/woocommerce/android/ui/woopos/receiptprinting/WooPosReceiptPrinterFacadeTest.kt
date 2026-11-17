package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.LabeledText
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private val buildThermalReceipt: WooPosBuildThermalReceipt = mock {
        on { invoke(ORDER_ID) } doReturn RECEIPT
    }

    private val sut = WooPosReceiptPrinterFacade(
        receiptPrinterManager = receiptPrinterManager,
        buildThermalReceipt = buildThermalReceipt,
        thermalReceiptRenderer = WooPosThermalReceiptRenderer(),
    )

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
    fun `given an order, when printReceipt is called, then the rendered receipt is printed`() = runTest {
        // WHEN
        val result = sut.printReceipt(ORDER_ID)

        // THEN
        verify(receiptPrinterManager).printReceipt(RECEIPT_TEXT)
        assertThat(result).isEqualTo(ReceiptPrinterResult.Success)
    }

    @Test
    fun `given the printer fails, when printReceipt is called, then the manager result is returned`() = runTest {
        // GIVEN
        whenever(receiptPrinterManager.printReceipt(RECEIPT_TEXT))
            .thenReturn(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))

        // WHEN
        val result = sut.printReceipt(ORDER_ID)

        // THEN
        assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PaperEmpty))
    }

    @Test
    fun `given the order is not found, when printReceipt is called, then it fails without printing`() = runTest {
        // GIVEN
        whenever(buildThermalReceipt(ORDER_ID)).thenReturn(null)

        // WHEN
        val result = sut.printReceipt(ORDER_ID)

        // THEN
        assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrintFailed()))
        verify(receiptPrinterManager, never()).printReceipt(any())
    }

    private companion object {
        const val ORDER_ID = 1042L
        val DEVICE = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
        val RECEIPT = WooPosThermalReceipt(
            storeName = "Everyday Pay",
            storeAddress = null,
            storePhone = null,
            storeEmail = null,
            datePaid = LabeledText(label = "Date Paid", text = "Nov 12, 2026, 2:30 PM"),
            orderNumber = "Order #1042",
            paymentMethod = null,
            lineItems = listOf(Row(label = "Coffee Mug x1", amount = "$12.00")),
            totals = listOf(
                Row(label = "Subtotal", amount = "$12.00"),
                Row(label = "Amount Paid", amount = "$12.00"),
            ),
            orderNote = LabeledText(label = "Notes", text = "Thanks!"),
            refundPolicy = null,
        )
        val RECEIPT_TEXT = """
                              Everyday Pay
            ------------------------------------------------
            Date Paid
            Nov 12, 2026, 2:30 PM
            Order #1042
            ------------------------------------------------
            Coffee Mug x1                             $12.00
            ------------------------------------------------
            Subtotal                                  $12.00
            Amount Paid                               $12.00
            ------------------------------------------------
            Notes
            Thanks!
        """.trimIndent() + "\n"
    }
}
