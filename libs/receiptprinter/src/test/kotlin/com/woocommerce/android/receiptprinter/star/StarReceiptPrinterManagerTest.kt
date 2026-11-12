package com.woocommerce.android.receiptprinter.star

import android.os.Build
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarIO10CommunicationException
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarIO10IllegalHostDeviceStateException
import com.starmicronics.stario10.StarIO10InUseException
import com.starmicronics.stario10.StarIO10NotFoundException
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterInformation
import com.starmicronics.stario10.StarPrinterInformationBluetooth
import com.starmicronics.stario10.StarPrinterInformationBluetoothLE
import com.starmicronics.stario10.StarPrinterInformationDetail
import com.starmicronics.stario10.StarPrinterModel
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus.Connected
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus.Connecting
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus.Disconnected
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus.Disconnecting
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent.Failed
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent.PrinterFound
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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
    fun `when discover is collected, then the discovery time and the callback are set before the SDK discovery starts`() =
        runTest {
            // WHEN
            collectDiscovery()

            // THEN
            inOrder(discoveryManager) {
                verify(discoveryManager).discoveryTime = 30_000
                verify(discoveryManager).callback = any()
                verify(discoveryManager).startDiscovery()
            }
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
    fun `given a printer on an interface other than Bluetooth, when it is found, then no Bluetooth name is used`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            collectDiscovery(events)

            // WHEN
            discoveryManager.capturedCallback().onPrinterFound(
                starPrinter(
                    interfaceType = InterfaceType.Lan,
                    information = printerInformation(
                        classicName = "Classic name",
                        leName = "LE name",
                        model = StarPrinterModel.mC_Print3,
                    ),
                )
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
    fun `given the discovery time passes while a failure is being reported, when a printer name cannot be read, then a single discovery failure is emitted`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val job = collectDiscovery(events)
            whenever(discoveryManager.stopDiscovery()).doAnswer { testScheduler.advanceTimeBy(35_001) }
            val printer = mock<StarPrinter> {
                on { connectionSettings } doReturn StarConnectionSettings(InterfaceType.Bluetooth, PRINTER_ID)
                on { information } doThrow SecurityException("BLUETOOTH_CONNECT")
            }

            // WHEN
            discoveryManager.capturedCallback().onPrinterFound(printer)

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
            assertThat(job.isCompleted).isTrue()
        }

    @Test
    fun `given the SDK reports that discovery finished as soon as it is stopped, when discovery fails, then the failure is still emitted`() =
        runTest {
            // GIVEN
            val events = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val job = collectDiscovery(events)
            val callback = discoveryManager.capturedCallback()
            whenever(discoveryManager.stopDiscovery()).doAnswer { callback.onDiscoveryFinished() }

            // WHEN
            advanceTimeBy(35_001)

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
            assertThat(job.isCompleted).isTrue()
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
    fun `given printers still waiting for a slow collector, when discovery fails, then they are delivered before the failure`() =
        runTest {
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
            callback.onPrinterFound(starPrinter(identifier = PRINTER_ID))
            callback.onPrinterFound(starPrinter(identifier = OTHER_PRINTER_ID))

            // WHEN
            advanceTimeBy(35_001)
            collectorCanProceed.complete(Unit)

            // THEN
            assertThat(events).containsExactly(
                PrinterFound(ReceiptPrinterDevice(id = PRINTER_ID, name = PRINTER_ID)),
                PrinterFound(ReceiptPrinterDevice(id = OTHER_PRINTER_ID, name = OTHER_PRINTER_ID)),
                Failed(ReceiptPrinterError.DiscoveryFailure),
            )
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
    fun `given a superseded discovery, when its SDK reports that it finished, then the newer discovery keeps running`() =
        runTest {
            // GIVEN
            collectDiscovery()
            val earlierCallback = discoveryManager.capturedCallback()
            val newerEvents = mutableListOf<ReceiptPrinterDiscoveryEvent>()
            val newerJob = collectDiscovery(newerEvents)

            // WHEN
            earlierCallback.onDiscoveryFinished()
            newerDiscoveryManager.capturedCallback().onPrinterFound(starPrinter(identifier = OTHER_PRINTER_ID))

            // THEN
            assertThat(newerJob.isActive).isTrue()
            verify(newerDiscoveryManager, never()).stopDiscovery()
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
    fun `given starting discovery throws a SecurityException, when discover is collected, then it emits a discovery failure and completes`() =
        runTest {
            // GIVEN
            whenever(discoveryManager.startDiscovery()).doAnswer { throw SecurityException("BLUETOOTH_SCAN") }

            // WHEN
            val events = sut.discover().toList()

            // THEN
            assertThat(events).containsExactly(Failed(ReceiptPrinterError.DiscoveryFailure))
        }

    @Test
    fun `given a discovered printer, when connect is called, then the status is connecting until the printer opens and connected after`() =
        runTest {
            // GIVEN
            val open = CompletableDeferred<Unit>()
            discover(starPrinter(open = open))

            // WHEN
            val result = connectAsync(DEVICE)
            val statusWhileOpening = sut.connectionStatus.value
            open.complete(Unit)
            runCurrent()

            // THEN
            assertThat(statusWhileOpening).isEqualTo(Connecting(DEVICE))
            assertThat(result.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(DEVICE))
        }

    @Test
    fun `when connect is called, then a printer delegate is assigned before the printer is opened`() = runTest {
        // GIVEN
        val printer = starPrinter()
        discover(printer)

        // WHEN
        sut.connect(DEVICE)

        // THEN
        inOrder(printer) {
            verify(printer).printerDelegate = any()
            verify(printer).openAsync()
        }
    }

    @Test
    fun `given a device that was never discovered, when connect is called, then it fails with printer not found`() =
        runTest {
            // WHEN
            val result = sut.connect(DEVICE)

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given the SDK cannot find the printer, when connect is called, then it fails with printer not found and the status is disconnected`() =
        runTest {
            // GIVEN
            discover(starPrinter(open = failed(StarIO10NotFoundException("not found"))))

            // WHEN
            val result = sut.connect(DEVICE)

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given the printer is in use, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            assertConnectFailsWithConnectionLost(StarIO10InUseException("in use"))
        }

    @Test
    fun `given communication with the printer fails, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            assertConnectFailsWithConnectionLost(StarIO10CommunicationException("communication"))
        }

    @Test
    fun `given the Bluetooth permission is missing, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            assertConnectFailsWithConnectionLost(SecurityException("BLUETOOTH_CONNECT"))
        }

    @Test
    fun `given opening throws another SDK error, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            assertConnectFailsWithConnectionLost(StarIO10Exception("unknown"))
        }

    @Test
    fun `given opening throws an unexpected error, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            assertConnectFailsWithConnectionLost(IllegalStateException("unexpected"))
        }

    @Test
    fun `given the SDK cancels the open, when connect is called, then it fails with connection lost and the status is disconnected`() =
        runTest {
            // GIVEN
            discover(starPrinter(open = CompletableDeferred<Unit>().apply { cancel() }))

            // WHEN
            val result = sut.connect(DEVICE)

            // THEN
            assertThat(result).isInstanceOfSatisfying(ReceiptPrinterResult.Failure::class.java) {
                assertThat(it.error).isInstanceOf(ReceiptPrinterError.ConnectionLost::class.java)
            }
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given a connected printer, when a second printer is connected, then the first is closed before the second is opened`() =
        runTest {
            // GIVEN
            val firstClose = CompletableDeferred<Unit>()
            val first = starPrinter(identifier = PRINTER_ID, close = firstClose)
            val second = starPrinter(identifier = OTHER_PRINTER_ID)
            discover(first, second)
            sut.connect(DEVICE)

            // WHEN
            val result = connectAsync(OTHER_DEVICE)
            val statusWhileClosingFirst = sut.connectionStatus.value
            verify(first).closeAsync()
            verify(second, never()).openAsync()
            firstClose.complete(Unit)
            runCurrent()

            // THEN
            assertThat(statusWhileClosingFirst).isEqualTo(Connecting(OTHER_DEVICE))
            verify(second).openAsync()
            assertThat(result.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
        }

    @Test
    fun `given a connect in flight, when a newer connect is called, then it waits for the earlier one, which closes its late connection and never reports connected`() =
        runTest {
            // GIVEN
            val firstOpen = CompletableDeferred<Unit>()
            val secondOpen = CompletableDeferred<Unit>()
            val first = starPrinter(identifier = PRINTER_ID, open = firstOpen)
            val second = starPrinter(identifier = OTHER_PRINTER_ID, open = secondOpen)
            discover(first, second)
            val statuses = collectStatuses()
            val firstResult = connectAsync(DEVICE)

            // WHEN
            val secondResult = connectAsync(OTHER_DEVICE)
            verify(second, never()).openAsync()
            firstOpen.complete(Unit)
            runCurrent()

            // THEN
            assertThat(firstResult.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            inOrder(first, second) {
                verify(first).closeAsync()
                verify(second).openAsync()
            }
            assertThat(sut.connectionStatus.value).isEqualTo(Connecting(OTHER_DEVICE))

            secondOpen.complete(Unit)
            runCurrent()
            assertThat(secondResult.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
            assertThat(statuses).doesNotContain(Connected(DEVICE))
        }

    @Test
    fun `given a superseded connect, when its printer fails to open, then the newer connect stays connecting and then connects`() =
        runTest {
            // GIVEN
            val firstOpen = CompletableDeferred<Unit>()
            val first = starPrinter(identifier = PRINTER_ID, open = firstOpen)
            val secondOpen = CompletableDeferred<Unit>()
            val second = starPrinter(identifier = OTHER_PRINTER_ID, open = secondOpen)
            discover(first, second)
            val firstResult = connectAsync(DEVICE)
            val secondResult = connectAsync(OTHER_DEVICE)

            // WHEN
            firstOpen.completeExceptionally(StarIO10NotFoundException("not found"))
            runCurrent()

            // THEN
            assertThat(firstResult.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            assertThat(sut.connectionStatus.value).isEqualTo(Connecting(OTHER_DEVICE))

            secondOpen.complete(Unit)
            runCurrent()
            assertThat(secondResult.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
        }

    @Test
    fun `given a connect superseded while it waits for an earlier one, when the earlier one settles, then only the newest printer is opened`() =
        runTest {
            // GIVEN
            val firstOpen = CompletableDeferred<Unit>()
            val first = starPrinter(identifier = PRINTER_ID, open = firstOpen)
            val second = starPrinter(identifier = OTHER_PRINTER_ID)
            val third = starPrinter(identifier = THIRD_PRINTER_ID)
            discover(first, second, third)
            connectAsync(DEVICE)
            val secondResult = connectAsync(OTHER_DEVICE)
            val thirdResult = connectAsync(THIRD_DEVICE)

            // WHEN
            firstOpen.complete(Unit)
            runCurrent()

            // THEN
            verify(second, never()).openAsync()
            assertThat(secondResult.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            assertThat(thirdResult.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(THIRD_DEVICE))
        }

    @Test
    fun `given a connect in flight, when it is cancelled, then it closes its late connection and the status ends at disconnected`() =
        runTest {
            // GIVEN
            val open = CompletableDeferred<Unit>()
            val printer = starPrinter(open = open)
            discover(printer)
            val statuses = collectStatuses()
            val result = connectAsync(DEVICE)

            // WHEN
            result.cancel()
            open.complete(Unit)
            runCurrent()

            // THEN
            assertThat(result.isCompleted).isTrue()
            assertThat(result.isCancelled).isTrue()
            verify(printer).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
            assertThat(statuses).doesNotContain(Connected(DEVICE))
        }

    @Test
    fun `given a connect cancelled while it waits for an earlier one, when the earlier one settles, then its printer is never opened and the status ends at disconnected`() =
        runTest {
            // GIVEN
            val firstOpen = CompletableDeferred<Unit>()
            val first = starPrinter(identifier = PRINTER_ID, open = firstOpen)
            val second = starPrinter(identifier = OTHER_PRINTER_ID)
            discover(first, second)
            connectAsync(DEVICE)
            val secondResult = connectAsync(OTHER_DEVICE)

            // WHEN
            secondResult.cancel()
            firstOpen.complete(Unit)
            runCurrent()

            // THEN
            verify(first).closeAsync()
            verify(second, never()).openAsync()
            assertThat(secondResult.isCompleted).isTrue()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given no printer is connected, when disconnect is called, then the status stays disconnected`() = runTest {
        // GIVEN
        val printer = starPrinter()
        discover(printer)
        val statuses = collectStatuses()

        // WHEN
        sut.disconnect()

        // THEN
        assertThat(statuses).containsExactly(Disconnected)
        verify(printer, never()).closeAsync()
    }

    @Test
    fun `given a connected printer, when disconnect is called, then the status is disconnecting until the printer closes and disconnected after`() =
        runTest {
            // GIVEN
            val close = CompletableDeferred<Unit>()
            val printer = starPrinter(close = close)
            discover(printer)
            sut.connect(DEVICE)

            // WHEN
            val disconnecting = disconnectAsync()
            val statusWhileClosing = sut.connectionStatus.value
            close.complete(Unit)
            runCurrent()

            // THEN
            verify(printer).closeAsync()
            assertThat(statusWhileClosing).isEqualTo(Disconnecting)
            assertThat(disconnecting.isCompleted).isTrue()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given closing the printer fails, when disconnect is called, then nothing is thrown and the status is disconnected`() =
        runTest {
            // GIVEN
            discover(starPrinter(close = failed(StarIO10CommunicationException("already gone"))))
            sut.connect(DEVICE)

            // WHEN
            sut.disconnect()

            // THEN
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given a connect in flight, when disconnect is called, then the connect closes its late connection and the status ends at disconnected`() =
        runTest {
            // GIVEN
            val open = CompletableDeferred<Unit>()
            val printer = starPrinter(open = open)
            discover(printer)
            val statuses = collectStatuses()
            val result = connectAsync(DEVICE)

            // WHEN
            val disconnecting = disconnectAsync()
            val statusWhileOpening = sut.connectionStatus.value
            open.complete(Unit)
            runCurrent()

            // THEN
            assertThat(statusWhileOpening).isEqualTo(Disconnecting)
            assertThat(result.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            verify(printer).closeAsync()
            assertThat(disconnecting.isCompleted).isTrue()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
            assertThat(statuses).doesNotContain(Connected(DEVICE))
        }

    @Test
    fun `given a disconnect waiting for a connect in flight, when the disconnect is cancelled, then the status still ends at disconnected`() =
        runTest {
            // GIVEN
            val open = CompletableDeferred<Unit>()
            val printer = starPrinter(open = open)
            discover(printer)
            connectAsync(DEVICE)
            val disconnecting = disconnectAsync()

            // WHEN
            disconnecting.cancel()
            open.complete(Unit)
            runCurrent()

            // THEN
            verify(printer).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given a disconnect in flight, when a newer connect is called, then the status ends at connected`() = runTest {
        // GIVEN
        val firstClose = CompletableDeferred<Unit>()
        val first = starPrinter(identifier = PRINTER_ID, close = firstClose)
        val second = starPrinter(identifier = OTHER_PRINTER_ID)
        discover(first, second)
        sut.connect(DEVICE)
        disconnectAsync()

        // WHEN
        val result = connectAsync(OTHER_DEVICE)
        firstClose.complete(Unit)
        runCurrent()

        // THEN
        assertThat(result.getCompleted()).isEqualTo(ReceiptPrinterResult.Success)
        assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
    }

    @Test
    fun `given a status collector that connects a second printer as soon as the first is connecting, when connect is called, then the second printer stays connected`() =
        runTest {
            // GIVEN
            val first = starPrinter(identifier = PRINTER_ID)
            val second = starPrinter(identifier = OTHER_PRINTER_ID)
            discover(first, second)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                sut.connectionStatus.first { it == Connecting(DEVICE) }
                sut.connect(OTHER_DEVICE)
            }

            // WHEN
            val result = sut.connect(DEVICE)

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            verify(first, never()).openAsync()
            verify(second, never()).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
        }

    @Test
    fun `given a status collector that connects a second printer as soon as the first is disconnecting, when disconnect is called, then the second printer stays connected`() =
        runTest {
            // GIVEN
            val first = starPrinter(identifier = PRINTER_ID)
            val second = starPrinter(identifier = OTHER_PRINTER_ID)
            discover(first, second)
            sut.connect(DEVICE)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                sut.connectionStatus.first { it == Disconnecting }
                sut.connect(OTHER_DEVICE)
            }

            // WHEN
            sut.disconnect()

            // THEN
            verify(first).closeAsync()
            verify(second, never()).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(OTHER_DEVICE))
        }

    @Test
    fun `given a connect in flight, when connect is called for a device that was never discovered, then the earlier connect closes its late connection and never reports connected`() =
        runTest {
            // GIVEN
            val open = CompletableDeferred<Unit>()
            val printer = starPrinter(open = open)
            discover(printer)
            val statuses = collectStatuses()
            val earlierResult = connectAsync(DEVICE)

            // WHEN
            val result = connectAsync(OTHER_DEVICE)
            open.complete(Unit)
            runCurrent()

            // THEN
            assertThat(earlierResult.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected))
            verify(printer).closeAsync()
            assertThat(result.getCompleted())
                .isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
            assertThat(statuses).doesNotContain(Connected(DEVICE))
        }

    @Test
    fun `given a connected printer, when connect is called for a device that was never discovered, then the printer is closed and the status is disconnected`() =
        runTest {
            // GIVEN
            val printer = starPrinter()
            discover(printer)
            sut.connect(DEVICE)

            // WHEN
            val result = sut.connect(OTHER_DEVICE)

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            verify(printer).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given a connect for an unknown device superseded by a status collector, when it ends, then it fails with printer not found and the newer printer stays connected`() =
        runTest {
            // GIVEN
            val printer = starPrinter()
            discover(printer)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                sut.connectionStatus.first { it == Connecting(OTHER_DEVICE) }
                sut.connect(DEVICE)
            }

            // WHEN
            val result = sut.connect(OTHER_DEVICE)

            // THEN
            assertThat(result).isEqualTo(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
            verify(printer, never()).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(DEVICE))
        }

    @Test
    fun `given the caller is cancelled while connected is being reported, when connect ends, then it is cancelled, the printer is closed and the status is disconnected`() =
        runTest {
            // GIVEN
            val printer = starPrinter()
            discover(printer)
            val result = backgroundScope.async { sut.connect(DEVICE) }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                sut.connectionStatus.first { it == Connected(DEVICE) }
                result.cancel()
            }

            // WHEN
            runCurrent()

            // THEN
            assertThat(result.isCompleted).isTrue()
            assertThat(result.isCancelled).isTrue()
            verify(printer).closeAsync()
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
        }

    @Test
    fun `given closing the printer failed, when it is connected again, then the close is retried before the printer is reopened`() =
        runTest {
            // GIVEN
            val printer = starPrinter()
            whenever(printer.closeAsync())
                .doReturn(failed(StarIO10CommunicationException("close failed")), CompletableDeferred(Unit))
            discover(printer)
            sut.connect(DEVICE)
            sut.disconnect()

            // WHEN
            val result = sut.connect(DEVICE)

            // THEN
            inOrder(printer) {
                verify(printer).openAsync()
                verify(printer, times(2)).closeAsync()
                verify(printer).openAsync()
            }
            assertThat(result).isEqualTo(ReceiptPrinterResult.Success)
            assertThat(sut.connectionStatus.value).isEqualTo(Connected(DEVICE))
        }

    @Test
    fun `given the SDK cancels the close, when disconnect is called, then nothing is thrown and the status is disconnected`() =
        runTest {
            // GIVEN
            discover(starPrinter(close = CompletableDeferred<Unit>().apply { cancel() }))
            sut.connect(DEVICE)

            // WHEN
            sut.disconnect()

            // THEN
            assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
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

    private fun TestScope.discover(vararg printers: StarPrinter) {
        collectDiscovery()
        val callback = discoveryManager.capturedCallback()
        printers.forEach { callback.onPrinterFound(it) }
    }

    private fun TestScope.connectAsync(device: ReceiptPrinterDevice): Deferred<ReceiptPrinterResult> =
        backgroundScope.async { sut.connect(device) }.also { runCurrent() }

    private fun TestScope.disconnectAsync(): Job =
        backgroundScope.launch { sut.disconnect() }.also { runCurrent() }

    private fun TestScope.collectStatuses(): List<ReceiptPrinterConnectionStatus> {
        val statuses = mutableListOf<ReceiptPrinterConnectionStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.connectionStatus.toList(statuses) }
        return statuses
    }

    private suspend fun TestScope.assertConnectFailsWithConnectionLost(cause: Exception) {
        // GIVEN
        discover(starPrinter(open = failed(cause)))

        // WHEN
        val result = sut.connect(DEVICE)

        // THEN
        assertThat(result).isInstanceOfSatisfying(ReceiptPrinterResult.Failure::class.java) { failure ->
            assertThat(failure.error).isInstanceOfSatisfying(ReceiptPrinterError.ConnectionLost::class.java) {
                assertThat(it.cause).isInstanceOf(cause::class.java)
            }
        }
        assertThat(sut.connectionStatus.value).isEqualTo(Disconnected)
    }

    private fun failed(cause: Exception): Deferred<Unit> =
        CompletableDeferred<Unit>().apply { completeExceptionally(cause) }

    private fun StarDeviceDiscoveryManager.capturedCallback(): StarDeviceDiscoveryManager.Callback {
        val captor = argumentCaptor<StarDeviceDiscoveryManager.Callback>()
        verify(this).callback = captor.capture()
        return captor.firstValue
    }

    private fun starPrinter(
        identifier: String = PRINTER_ID,
        interfaceType: InterfaceType = InterfaceType.Bluetooth,
        information: StarPrinterInformation? = null,
        open: Deferred<Unit> = CompletableDeferred(Unit),
        close: Deferred<Unit> = CompletableDeferred(Unit),
    ): StarPrinter = mock {
        on { connectionSettings } doReturn StarConnectionSettings(interfaceType, identifier)
        on { this.information } doReturn information
        on { openAsync() } doReturn open
        on { closeAsync() } doReturn close
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
        const val THIRD_PRINTER_ID = "00:11:62:AA:BB:03"
        const val PRINTERS_BEYOND_DEFAULT_BUFFER = 100
        val DEVICE = ReceiptPrinterDevice(id = PRINTER_ID, name = PRINTER_ID)
        val OTHER_DEVICE = ReceiptPrinterDevice(id = OTHER_PRINTER_ID, name = OTHER_PRINTER_ID)
        val THIRD_DEVICE = ReceiptPrinterDevice(id = THIRD_PRINTER_ID, name = THIRD_PRINTER_ID)
    }
}
