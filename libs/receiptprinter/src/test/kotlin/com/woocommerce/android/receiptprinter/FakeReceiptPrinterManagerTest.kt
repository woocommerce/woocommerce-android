package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class FakeReceiptPrinterManagerTest {
    private val sut = FakeReceiptPrinterManager()

    @Test
    fun `when created, then the connection status is idle`() {
        assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Idle)
    }

    @Test
    fun `given discovered devices, when discover is collected, then the devices are emitted`() = runTest {
        // GIVEN
        sut.discoveredDevices = listOf(DEVICE)

        // WHEN
        val devices = sut.discover().toList()

        // THEN
        assertThat(devices).containsExactly(DEVICE)
    }

    @Test
    fun `when connect is called, then the status becomes connected to the device`() = runTest {
        // WHEN
        sut.connect(DEVICE)

        // THEN
        assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Connected(DEVICE))
    }

    @Test
    fun `given a connected printer, when disconnect is called, then the status becomes disconnected`() = runTest {
        // GIVEN
        sut.connect(DEVICE)

        // WHEN
        sut.disconnect()

        // THEN
        assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Disconnected)
    }

    @Test
    fun `given no connected printer, when print is called, then a printer not connected error is thrown`() = runTest {
        // WHEN
        val exception = assertFailsWith<ReceiptPrinterException> { sut.print("receipt") }

        // THEN
        assertThat(exception.error).isEqualTo(ReceiptPrinterError.PrinterNotConnected)
    }

    @Test
    fun `given a connected printer, when print is called, then the text is recorded`() = runTest {
        // GIVEN
        sut.connect(DEVICE)

        // WHEN
        sut.print("receipt")

        // THEN
        assertThat(sut.printedTexts).containsExactly("receipt")
    }

    private companion object {
        val DEVICE = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
    }
}
