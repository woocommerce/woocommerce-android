package com.woocommerce.android.receiptprinter.star

import android.os.Build
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarIO10IllegalHostDeviceStateException
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterInformation
import com.starmicronics.stario10.StarPrinterInformationBluetooth
import com.starmicronics.stario10.StarPrinterInformationBluetoothLE
import com.starmicronics.stario10.StarPrinterInformationDetail
import com.starmicronics.stario10.StarPrinterModel
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent.Failed
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent.PrinterFound
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StarReceiptPrinterManagerTest {
    private val discoveryManager: StarDeviceDiscoveryManager = mock()
    private val newerDiscoveryManager: StarDeviceDiscoveryManager = mock()
    private val starIO10: StarIO10Wrapper = mock {
        on { androidSdkVersion } doReturn Build.VERSION_CODES.S
        on { createDiscoveryManager(any()) }.doReturn(discoveryManager, newerDiscoveryManager)
    }

    private val sut = StarReceiptPrinterManager(starIO10)

    @Test
    fun `when the manager is created, then the SDK diagnostics upload is disabled`() {
        verify(starIO10).disableDiagnosticsUpload()
    }

    @Test
    fun `given Android 12 or newer, when discover is collected, then Bluetooth Classic and LE are scanned for 30 seconds`() =
        runTest {
            // WHEN
            collectDiscovery()

            // THEN
            verify(starIO10).createDiscoveryManager(listOf(InterfaceType.Bluetooth, InterfaceType.BluetoothLE))
            verify(discoveryManager).discoveryTime = 30_000
            verify(discoveryManager).startDiscovery()
        }

    @Test
    fun `given Android 11 or older, when discover is collected, then only Bluetooth Classic is scanned`() = runTest {
        // GIVEN
        whenever(starIO10.androidSdkVersion).thenReturn(Build.VERSION_CODES.R)

        // WHEN
        collectDiscovery()

        // THEN
        verify(starIO10).createDiscoveryManager(listOf(InterfaceType.Bluetooth))
    }

    @Test
    fun `given discover is not collected, when discover is called, then no SDK discovery starts`() {
        // WHEN
        sut.discover()

        // THEN
        verify(starIO10, never()).createDiscoveryManager(any())
    }

    @Test
    fun `given a Bluetooth Classic printer, when it is found, then it is emitted with its Bluetooth name`() = runTest {
        // GIVEN
        val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
        collectDiscovery(events)

        // WHEN
        discoveryManager.capturedCallback().onPrinterFound(
            starPrinter(information = printerInformation(classicName = "mC-Print3-A1", leName = "LE name"))
        )

        // THEN
        assertThat(events).containsExactly(PrinterFound(ReceiptPrinterDevice(id = PRINTER_ID, name = "mC-Print3-A1")))
    }

    @Test
    fun `given a Bluetooth LE printer, when it is found, then it is emitted with its Bluetooth LE name`() = runTest {
        // GIVEN
        val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
        collectDiscovery(events)

        // WHEN
        discoveryManager.capturedCallback().onPrinterFound(
            starPrinter(
                interfaceType = InterfaceType.BluetoothLE,
                information = printerInformation(classicName = "Classic name", leName = "TSP100IV-B2"),
            )
        )

        // THEN
        assertThat(events).containsExactly(PrinterFound(ReceiptPrinterDevice(id = PRINTER_ID, name = "TSP100IV-B2")))
    }

    @Test
    fun `given a printer without a Bluetooth name, when it is found, then it is emitted with its model name`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            collectDiscovery(events)

            // WHEN
            discoveryManager.capturedCallback().onPrinterFound(
                starPrinter(information = printerInformation(classicName = "", model = StarPrinterModel.mC_Print3))
            )

            // THEN
            assertThat(events).containsExactly(
                PrinterFound(ReceiptPrinterDevice(id = PRINTER_ID, name = StarPrinterModel.mC_Print3.modelName))
            )
        }

    @Test
    fun `given a printer without a name or a known model, when it is found, then it is emitted with its identifier`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            collectDiscovery(events)
            val callback = discoveryManager.capturedCallback()

            // WHEN
            callback.onPrinterFound(starPrinter(identifier = PRINTER_ID, information = printerInformation()))
            callback.onPrinterFound(starPrinter(identifier = OTHER_PRINTER_ID, information = null))

            // THEN
            assertThat(events).containsExactly(
                PrinterFound(ReceiptPrinterDevice(id = PRINTER_ID, name = PRINTER_ID)),
                PrinterFound(ReceiptPrinterDevice(id = OTHER_PRINTER_ID, name = OTHER_PRINTER_ID)),
            )
        }

    @Test
    fun `given reading the printer name needs a missing permission, when a printer is found, then discovery fails and stops`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val job = collectDiscovery(events)
            val printer = mock<StarPrinter> {
                on { connectionSettings } doReturn StarConnectionSettings(InterfaceType.Bluetooth, PRINTER_ID)
                on { information } doThrow SecurityException("BLUETOOTH_CONNECT")
            }

            // WHEN
            discoveryManager.capturedCallback().onPrinterFound(printer)

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
            assertThat(job.isCompleted).isTrue()
            verify(discoveryManager).stopDiscovery()
        }

    @Test
    fun `given a collector slower than the SDK, when many printers are found, then none is dropped`() = runTest {
        // GIVEN
        val collectorCanProceed = CompletableDeferred<Unit>()
        val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.discover().collect {
                collectorCanProceed.await()
                events += it
            }
        }
        val callback = discoveryManager.capturedCallback()

        // WHEN
        repeat(PRINTERS_BEYOND_DEFAULT_BUFFER) { callback.onPrinterFound(starPrinter(identifier = "printer-$it")) }
        collectorCanProceed.complete(Unit)

        // THEN
        assertThat(events).hasSize(PRINTERS_BEYOND_DEFAULT_BUFFER)
    }

    @Test
    fun `given the SDK never reports that discovery finished, when the discovery time has passed, then discovery fails and stops`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val job = collectDiscovery(events)

            // WHEN
            advanceTimeBy(30_000)
            val wasActiveAtDiscoveryTime = job.isActive
            advanceTimeBy(5_001)

            // THEN
            assertThat(wasActiveAtDiscoveryTime).isTrue()
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
            assertThat(job.isCompleted).isTrue()
            verify(discoveryManager).stopDiscovery()
        }

    @Test
    fun `given discover is collected, when the SDK finishes discovery, then the flow completes`() = runTest {
        // GIVEN
        val job = collectDiscovery()

        // WHEN
        discoveryManager.capturedCallback().onDiscoveryFinished()

        // THEN
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun `given discover is collected, when the collector is cancelled, then the SDK discovery is stopped`() = runTest {
        // GIVEN
        val job = collectDiscovery()
        verify(discoveryManager, never()).stopDiscovery()

        // WHEN
        job.cancel()

        // THEN
        verify(discoveryManager).stopDiscovery()
    }

    @Test
    fun `given stopping the SDK discovery throws, when the collector is cancelled, then nothing is thrown`() = runTest {
        // GIVEN
        whenever(discoveryManager.stopDiscovery()).doAnswer { throw StarIO10Exception("stop failed") }
        val job = collectDiscovery()

        // WHEN
        job.cancel()

        // THEN
        verify(discoveryManager).stopDiscovery()
    }

    @Test
    fun `given a discovery in progress, when discover is collected again, then the earlier session is stopped and its flow completes`() =
        runTest {
            // GIVEN
            val earlierEvents = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val earlierJob = collectDiscovery(earlierEvents)
            val earlierCallback = discoveryManager.capturedCallback()

            // WHEN
            val newerEvents = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val newerJob = collectDiscovery(newerEvents)
            val latePrinter = starPrinter(identifier = PRINTER_ID)
            earlierCallback.onPrinterFound(latePrinter)
            newerDiscoveryManager.capturedCallback().onPrinterFound(starPrinter(identifier = OTHER_PRINTER_ID))

            // THEN
            verify(discoveryManager).stopDiscovery()
            assertThat(earlierJob.isCompleted).isTrue()
            assertThat(earlierEvents).isEmpty()
            verify(latePrinter, never()).connectionSettings
            verify(newerDiscoveryManager).startDiscovery()
            verify(newerDiscoveryManager, never()).stopDiscovery()
            assertThat(newerJob.isActive).isTrue()
            assertThat(newerEvents).containsExactly(
                PrinterFound(ReceiptPrinterDevice(id = OTHER_PRINTER_ID, name = OTHER_PRINTER_ID))
            )
        }

    @Test
    fun `given Bluetooth is unavailable, when discover is collected, then it emits a discovery failure and completes`() =
        runTest {
            // GIVEN
            whenever(discoveryManager.startDiscovery()).doAnswer {
                throw mock<StarIO10IllegalHostDeviceStateException>()
            }

            // WHEN
            val events = sut.discover().toList()

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
        }

    @Test
    fun `given the Bluetooth permission is missing, when discover is collected, then it emits a discovery failure and completes`() =
        runTest {
            // GIVEN
            whenever(discoveryManager.startDiscovery()).doAnswer { throw SecurityException("BLUETOOTH_SCAN") }

            // WHEN
            val events = sut.discover().toList()

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
        }

    @Test
    fun `given starting discovery throws another SDK error, when discover is collected, then it emits a discovery failure and completes`() =
        runTest {
            // GIVEN
            whenever(discoveryManager.startDiscovery()).doAnswer { throw StarIO10Exception("unknown") }

            // WHEN
            val events = sut.discover().toList()

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
        }

    @Test
    fun `given creating the SDK discovery manager throws, when discover is collected, then it emits a discovery failure and completes`() =
        runTest {
            // GIVEN
            whenever(starIO10.createDiscoveryManager(any())).doAnswer {
                throw mock<StarIO10IllegalHostDeviceStateException>()
            }

            // WHEN
            val events = sut.discover().toList()

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
        }

    @Test
    fun `when connect is called, then it fails with printer not connected and the status stays disconnected`() =
        runTest {
            // WHEN
            val result = sut.connect(ReceiptPrinterDevice(id = PRINTER_ID, name = PRINTER_ID))

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            assertThat(sut.connectionStatus.value).isEqualTo(ReceiptPrinterConnectionStatus.Disconnected)
        }

    @Test
    fun `when printReceipt is called, then it fails with printer not connected`() = runTest {
        // WHEN
        val result = sut.printReceipt("receipt")

        // THEN
        assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
    }

    private fun TestScope.collectDiscovery(events: MutableList<ReceiptPrinterDiscoveryEvent> = mutableListOf()): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.discover().toList(events) }

    private fun StarDeviceDiscoveryManager.capturedCallback(): StarDeviceDiscoveryManager.Callback {
        val captor = argumentCaptor<StarDeviceDiscoveryManager.Callback>()
        verify(this).callback = captor.capture()
        return captor.firstValue
    }

    private fun starPrinter(
        identifier: String = PRINTER_ID,
        interfaceType: InterfaceType = InterfaceType.Bluetooth,
        information: StarPrinterInformation? = null,
    ): StarPrinter = mock {
        on { connectionSettings } doReturn StarConnectionSettings(interfaceType, identifier)
        on { this.information } doReturn information
    }

    private fun printerInformation(
        classicName: String? = null,
        leName: String? = null,
        model: StarPrinterModel = StarPrinterModel.Unknown,
    ): StarPrinterInformation {
        val bluetooth = mock<StarPrinterInformationBluetooth> { on { deviceName } doReturn classicName }
        val bluetoothLE = mock<StarPrinterInformationBluetoothLE> { on { deviceName } doReturn leName }
        val detail = mock<StarPrinterInformationDetail> {
            on { this.bluetooth } doReturn bluetooth
            on { this.bluetoothLE } doReturn bluetoothLE
        }
        return mock {
            on { this.detail } doReturn detail
            on { this.model } doReturn model
        }
    }

    private companion object {
        const val PRINTER_ID = "00:11:62:AA:BB:01"
        const val OTHER_PRINTER_ID = "00:11:62:AA:BB:02"
        const val PRINTERS_BEYOND_DEFAULT_BUFFER = 100
    }
}
