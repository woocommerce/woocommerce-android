package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.FakeReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterException
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosReceiptPrinterFacadeTest {
    @get:Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val receiptPrinterManager = FakeReceiptPrinterManager()

    private val sut = WooPosReceiptPrinterFacade(receiptPrinterManager)

    @Test
    fun `given the manager is connected, when reading connection status, then it mirrors the manager`() {
        // GIVEN
        receiptPrinterManager.setConnectionStatus(ReceiptPrinterConnectionStatus.Connected(DEVICE))

        // THEN
        assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Connected(DEVICE))
    }

    @Test
    fun `given the manager discovers devices, when discover is collected, then the devices are emitted`() = runTest {
        // GIVEN
        receiptPrinterManager.discoveredDevices = listOf(DEVICE)
        val devices = mutableListOf<ReceiptPrinterDevice>()

        // WHEN
        val collection = launch { sut.discover().toList(devices) }
        advanceUntilIdle()

        // THEN
        assertThat(devices).containsExactly(DEVICE)
        collection.cancel()
    }

    @Test
    fun `given discovery is in flight, when stopDiscovery is called, then the collection completes`() = runTest {
        // GIVEN
        val collection = launch { sut.discover().toList(mutableListOf()) }
        advanceUntilIdle()
        assertThat(receiptPrinterManager.isDiscovering).isTrue()

        // WHEN
        sut.stopDiscovery()
        advanceUntilIdle()

        // THEN
        assertThat(collection.isCompleted).isTrue()
        assertThat(receiptPrinterManager.isDiscovering).isFalse()
    }

    @Test
    fun `given the manager fails to discover, when discover is collected, then the error is propagated`() = runTest {
        // GIVEN
        receiptPrinterManager.discoveryError = ReceiptPrinterError.DiscoveryFailure

        // WHEN
        val exception = assertFailsWith<ReceiptPrinterException> { sut.discover().toList() }

        // THEN
        assertThat(exception.error).isEqualTo(ReceiptPrinterError.DiscoveryFailure)
    }

    @Test
    fun `when connect is called, then the manager connects to the device`() = runTest {
        // WHEN
        sut.connect(DEVICE)

        // THEN
        assertThat(receiptPrinterManager.connectionStatus.value)
            .isEqualTo(ReceiptPrinterConnectionStatus.Connected(DEVICE))
    }

    @Test
    fun `given the manager fails to connect, when connect is called, then the error is propagated`() = runTest {
        // GIVEN
        receiptPrinterManager.connectError = ReceiptPrinterError.PrinterNotFound

        // WHEN
        val exception = assertFailsWith<ReceiptPrinterException> { sut.connect(DEVICE) }

        // THEN
        assertThat(exception.error).isEqualTo(ReceiptPrinterError.PrinterNotFound)
    }

    @Test
    fun `given a connected printer, when disconnect is called, then the manager disconnects`() = runTest {
        // GIVEN
        sut.connect(DEVICE)

        // WHEN
        sut.disconnect()

        // THEN
        assertThat(receiptPrinterManager.disconnectCalls).isEqualTo(1)
        assertThat(receiptPrinterManager.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Disconnected)
    }

    @Test
    fun `given a connected printer, when printReceipt is called, then the text reaches the manager`() = runTest {
        // GIVEN
        sut.connect(DEVICE)

        // WHEN
        sut.printReceipt(RECEIPT_TEXT)

        // THEN
        assertThat(receiptPrinterManager.printedTexts).containsExactly(RECEIPT_TEXT)
    }

    @Test
    fun `given the manager fails to print, when printReceipt is called, then the error is propagated`() = runTest {
        // GIVEN
        sut.connect(DEVICE)
        receiptPrinterManager.printError = ReceiptPrinterError.PrintFailed(detail = "Printer offline")

        // WHEN
        val exception = assertFailsWith<ReceiptPrinterException> { sut.printReceipt(RECEIPT_TEXT) }

        // THEN
        assertThat(exception.error).isEqualTo(ReceiptPrinterError.PrintFailed(detail = "Printer offline"))
    }

    private companion object {
        val DEVICE = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
        const val RECEIPT_TEXT = "Total: 10.00"
    }
}
