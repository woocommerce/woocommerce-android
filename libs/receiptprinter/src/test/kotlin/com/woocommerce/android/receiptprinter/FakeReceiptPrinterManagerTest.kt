package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
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
        val devices = mutableListOf<ReceiptPrinterDevice>()

        // WHEN
        val collection = launch { sut.discover().toList(devices) }
        advanceUntilIdle()

        // THEN
        assertThat(devices).containsExactly(DEVICE)
        assertThat(sut.isDiscovering).isTrue()
        collection.cancel()
    }

    @Test
    fun `given discovery is in flight, when stopDiscovery is called, then the collection completes`() = runTest {
        // GIVEN
        val devices = mutableListOf<ReceiptPrinterDevice>()
        val collection = launch { sut.discover().toList(devices) }
        advanceUntilIdle()
        sut.emitDiscoveredDevice(DEVICE)

        // WHEN
        sut.stopDiscovery()
        advanceUntilIdle()

        // THEN
        assertThat(collection.isCompleted).isTrue()
        assertThat(devices).containsExactly(DEVICE)
        assertThat(sut.isDiscovering).isFalse()
    }

    @Test
    fun `given a discovery error, when discover is collected, then a discovery failure is thrown`() = runTest {
        // GIVEN
        sut.discoveryError = ReceiptPrinterError.DiscoveryFailure

        // WHEN
        val exception = assertFailsWith<ReceiptPrinterException> { sut.discover().toList() }

        // THEN
        assertThat(exception.error).isEqualTo(ReceiptPrinterError.DiscoveryFailure)
        assertThat(sut.isDiscovering).isFalse()
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
    fun `given no connected printer, when printReceipt is called, then a printer not connected error is thrown`() =
        runTest {
            // WHEN
            val exception = assertFailsWith<ReceiptPrinterException> { sut.printReceipt("receipt") }

            // THEN
            assertThat(exception.error).isEqualTo(ReceiptPrinterError.PrinterNotConnected)
        }

    @Test
    fun `given a connected printer, when printReceipt is called, then the text is recorded`() = runTest {
        // GIVEN
        sut.connect(DEVICE)

        // WHEN
        sut.printReceipt("receipt")

        // THEN
        assertThat(sut.printedTexts).containsExactly("receipt")
    }

    private companion object {
        val DEVICE = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
    }
}
