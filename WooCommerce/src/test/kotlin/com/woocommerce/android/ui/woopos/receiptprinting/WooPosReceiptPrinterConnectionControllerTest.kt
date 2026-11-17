package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.BluetoothOff
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Connecting
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.ConnectionFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.DiscoveryFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundMultiple
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundOne
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Idle
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.MissingBluetoothPermission
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.NoPrintersFound
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Searching
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosReceiptPrinterConnectionControllerTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val connectionStatus = MutableStateFlow<ReceiptPrinterConnectionStatus>(
        ReceiptPrinterConnectionStatus.Disconnected
    )
    private val discoveryEvents = Channel<ReceiptPrinterDiscoveryEvent>(Channel.UNLIMITED)
    private var isDiscovering = false
    private var searchesStarted = 0

    private val receiptPrinterFacade: WooPosReceiptPrinterFacade = mock {
        on { connectionStatus } doReturn connectionStatus
        on { discover() } doReturn discoveryEvents.receiveAsFlow()
            .onStart {
                isDiscovering = true
                searchesStarted++
            }
            .onCompletion { isDiscovering = false }
    }
    private val permissionUtils: WooPosPermissionUtils = mock {
        on { hasBluetoothScanAndConnectPermissions() } doReturn true
        on { isBluetoothEnabled() } doReturn true
    }

    @Test
    fun `when the controller is created, then the state is idle and nothing is searched`() = runTest {
        // WHEN
        val sut = createController()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
        verify(receiptPrinterFacade, never()).discover()
    }

    @Test
    fun `given Bluetooth permissions are missing, when setup starts, then the permission is asked for and nothing else is checked`() =
        runTest {
            // GIVEN
            whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
            val sut = createController()

            // WHEN
            sut.startSetup()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(MissingBluetoothPermission(permanentlyDenied = false))
            verify(permissionUtils, never()).isBluetoothEnabled()
            verify(receiptPrinterFacade, never()).discover()
        }

    @Test
    fun `given Bluetooth is off, when setup starts, then the state is Bluetooth off and nothing is searched`() = runTest {
        // GIVEN
        whenever(permissionUtils.isBluetoothEnabled()).thenReturn(false)
        val sut = createController()

        // WHEN
        sut.startSetup()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(BluetoothOff)
        verify(receiptPrinterFacade, never()).discover()
    }

    @Test
    fun `given Bluetooth is ready, when setup starts, then permissions, then the radio are checked before searching`() =
        runTest {
            // GIVEN
            val sut = createController()

            // WHEN
            sut.startSetup()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Searching)
            inOrder(permissionUtils, receiptPrinterFacade) {
                verify(permissionUtils).hasBluetoothScanAndConnectPermissions()
                verify(permissionUtils).isBluetoothEnabled()
                verify(receiptPrinterFacade).discover()
            }
        }

    @Test
    fun `given the permission can be asked again, when it is denied, then it is not marked as permanently denied`() =
        runTest {
            // GIVEN
            whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
            val sut = createController()
            sut.startSetup()

            // WHEN
            sut.onBluetoothPermissionResult(granted = false, shouldShowRationale = true)

            // THEN
            assertThat(sut.state.value).isEqualTo(MissingBluetoothPermission(permanentlyDenied = false))
        }

    @Test
    fun `given the permission cannot be asked again, when it is denied, then it is marked as permanently denied`() =
        runTest {
            // GIVEN
            whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
            val sut = createController()
            sut.startSetup()

            // WHEN
            sut.onBluetoothPermissionResult(granted = false, shouldShowRationale = false)

            // THEN
            assertThat(sut.state.value).isEqualTo(MissingBluetoothPermission(permanentlyDenied = true))
        }

    @Test
    fun `given the permission was missing, when it is granted, then the search starts`() = runTest {
        // GIVEN
        whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
        val sut = createController()
        sut.startSetup()
        whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(true)

        // WHEN
        sut.onBluetoothPermissionResult(granted = true, shouldShowRationale = false)
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Searching)
        verify(receiptPrinterFacade).discover()
    }

    @Test
    fun `given the permission was permanently denied, when it is granted in the settings and rechecked, then the search starts`() =
        runTest {
            // GIVEN
            whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
            val sut = createController()
            sut.startSetup()
            sut.onBluetoothPermissionResult(granted = false, shouldShowRationale = false)
            whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(true)

            // WHEN
            sut.recheckBluetoothRequirement()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Searching)
            verify(receiptPrinterFacade).discover()
        }

    @Test
    fun `given setup was cancelled, when a permission result arrives, then setup is not started again`() = runTest {
        // GIVEN
        whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(false)
        val sut = createController()
        sut.startSetup()
        sut.cancelSetup()
        whenever(permissionUtils.hasBluetoothScanAndConnectPermissions()).thenReturn(true)

        // WHEN
        sut.onBluetoothPermissionResult(granted = true, shouldShowRationale = false)
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
        assertThat(searchesStarted).isEqualTo(0)
    }

    @Test
    fun `given Bluetooth was off, when it is turned on and rechecked, then the search starts`() = runTest {
        // GIVEN
        whenever(permissionUtils.isBluetoothEnabled()).thenReturn(false)
        val sut = createController()
        sut.startSetup()
        whenever(permissionUtils.isBluetoothEnabled()).thenReturn(true)

        // WHEN
        sut.recheckBluetoothRequirement()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Searching)
        verify(receiptPrinterFacade).discover()
    }

    @Test
    fun `given Bluetooth is still off, when rechecked, then the state stays Bluetooth off`() = runTest {
        // GIVEN
        whenever(permissionUtils.isBluetoothEnabled()).thenReturn(false)
        val sut = createController()
        sut.startSetup()

        // WHEN
        sut.recheckBluetoothRequirement()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(BluetoothOff)
        verify(receiptPrinterFacade, never()).discover()
    }

    @Test
    fun `given a printer was found, when the Bluetooth requirement is rechecked, then the search is not restarted`() =
        runTest {
            // GIVEN
            val sut = createController()
            sut.startSetup()
            find(PRINTER)

            // WHEN
            sut.recheckBluetoothRequirement()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
            assertThat(searchesStarted).isEqualTo(1)
        }

    @Test
    fun `given a search is running, when one printer is found, then the state is found one`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()

        // WHEN
        find(PRINTER)

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
    }

    @Test
    fun `given one printer was found, when the same printer is found again, then the state stays found one`() =
        runTest {
            // GIVEN
            val sut = createController()
            sut.startSetup()
            find(PRINTER)

            // WHEN
            find(PRINTER)

            // THEN
            assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
        }

    @Test
    fun `given one printer was found, when more printers are found, then the state stays found multiple`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        find(OTHER_PRINTER)
        val stateAfterTwo = sut.state.value
        find(THIRD_PRINTER)

        // THEN
        assertThat(stateAfterTwo).isEqualTo(FoundMultiple(listOf(PRINTER, OTHER_PRINTER)))
        assertThat(sut.state.value).isEqualTo(FoundMultiple(listOf(PRINTER, OTHER_PRINTER, THIRD_PRINTER)))
    }

    @Test
    fun `given no printer was found, when the search ends, then the state is no printers found`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        runCurrent()

        // WHEN
        discoveryEvents.close()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(NoPrintersFound)
    }

    @Test
    fun `given a printer was found, when the search ends, then the printer stays offered`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        discoveryEvents.close()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
    }

    @Test
    fun `given a search is running, when it fails, then the state is discovery failed`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()

        // WHEN
        discoveryEvents.trySend(ReceiptPrinterDiscoveryEvent.Failed(ReceiptPrinterError.DiscoveryFailure))
        discoveryEvents.close()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(DiscoveryFailed)
    }

    @Test
    fun `given a printer was found, when the search fails, then the printer stays offered`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        discoveryEvents.trySend(ReceiptPrinterDiscoveryEvent.Failed(ReceiptPrinterError.DiscoveryFailure))
        discoveryEvents.close()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
    }

    @Test
    fun `given the search is still running, when keeping searching, then the offered printer is skipped without a new search`() =
        runTest {
            // GIVEN
            val sut = createController()
            sut.startSetup()
            find(PRINTER)

            // WHEN
            sut.keepSearching()
            val stateAfterKeepSearching = sut.state.value
            find(PRINTER)
            val stateAfterSkippedPrinter = sut.state.value
            find(OTHER_PRINTER)

            // THEN
            assertThat(stateAfterKeepSearching).isEqualTo(Searching)
            assertThat(stateAfterSkippedPrinter).isEqualTo(Searching)
            assertThat(sut.state.value).isEqualTo(FoundOne(OTHER_PRINTER))
            assertThat(searchesStarted).isEqualTo(1)
        }

    @Test
    fun `given the only printer was skipped, when the search ends, then the state is no printers found`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)
        sut.keepSearching()

        // WHEN
        discoveryEvents.close()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(NoPrintersFound)
    }

    @Test
    fun `given the search has ended, when keeping searching, then a new search skips the offered printer`() = runTest {
        // GIVEN
        whenever(receiptPrinterFacade.discover()).thenReturn(
            flowOf(ReceiptPrinterDiscoveryEvent.PrinterFound(PRINTER)),
            flowOf(
                ReceiptPrinterDiscoveryEvent.PrinterFound(PRINTER),
                ReceiptPrinterDiscoveryEvent.PrinterFound(OTHER_PRINTER),
            ),
        )
        val sut = createController()
        sut.startSetup()
        runCurrent()

        // WHEN
        sut.keepSearching()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(OTHER_PRINTER))
        verify(receiptPrinterFacade, times(2)).discover()
    }

    @Test
    fun `given a printer was skipped, when setup starts again, then the printer is offered again`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)
        sut.keepSearching()

        // WHEN
        sut.startSetup()
        find(PRINTER)

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
        assertThat(searchesStarted).isEqualTo(2)
    }

    @Test
    fun `given no printer is offered, when keeping searching, then nothing changes`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)
        find(OTHER_PRINTER)

        // WHEN
        sut.keepSearching()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundMultiple(listOf(PRINTER, OTHER_PRINTER)))
        assertThat(searchesStarted).isEqualTo(1)
    }

    @Test
    fun `given a printer was found, when connecting, then the search is stopped before the connection starts`() =
        runTest {
            // GIVEN
            val calls = mutableListOf<String>()
            whenever(receiptPrinterFacade.discover()).thenReturn(
                flow {
                    emit(ReceiptPrinterDiscoveryEvent.PrinterFound(PRINTER))
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) { yield() }
                        calls += "search stopped"
                    }
                }
            )
            whenever(receiptPrinterFacade.connect(PRINTER)).doSuspendableAnswer {
                calls += "connect"
                ReceiptPrinterResult.Success
            }
            val sut = createController()
            sut.startSetup()
            runCurrent()

            // WHEN
            sut.connect(PRINTER)
            runCurrent()

            // THEN
            assertThat(calls).containsExactly("search stopped", "connect")
        }

    @Test
    fun `given a connection is in progress, when another printer is found, then the state stays connecting`() =
        runTest {
            // GIVEN
            whenever(receiptPrinterFacade.connect(PRINTER)).doSuspendableAnswer { awaitCancellation() }
            val sut = createController()
            sut.startSetup()
            find(PRINTER)

            // WHEN
            sut.connect(PRINTER)
            find(OTHER_PRINTER)

            // THEN
            assertThat(sut.state.value).isEqualTo(Connecting(PRINTER))
        }

    @Test
    fun `given a connection is in progress, when it succeeds, then the state is idle`() = runTest {
        // GIVEN
        whenever(receiptPrinterFacade.connect(PRINTER)).thenReturn(ReceiptPrinterResult.Success)
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        sut.connect(PRINTER)
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
    }

    @Test
    fun `given a connection is in progress, when it fails, then the state is connection failed`() = runTest {
        // GIVEN
        whenever(receiptPrinterFacade.connect(PRINTER))
            .thenReturn(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        sut.connect(PRINTER)
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(ConnectionFailed)
    }

    @Test
    fun `given a search is running, when setup is cancelled, then the search stops and the state is idle`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        find(PRINTER)

        // WHEN
        sut.cancelSetup()
        find(OTHER_PRINTER)

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
        assertThat(isDiscovering).isFalse()
    }

    @Test
    fun `given a connection is in progress, when setup is cancelled, then a late failure is not reported`() = runTest {
        // GIVEN
        val connectResult = CompletableDeferred<ReceiptPrinterResult>()
        whenever(receiptPrinterFacade.connect(PRINTER)).doSuspendableAnswer { connectResult.await() }
        val sut = createController()
        sut.startSetup()
        find(PRINTER)
        sut.connect(PRINTER)
        runCurrent()

        // WHEN
        sut.cancelSetup()
        connectResult.complete(ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound))
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
    }

    @Test
    fun `given setup restarts while a connection succeeds, when the connection returns, then the new search is kept`() =
        runTest {
            // GIVEN
            lateinit var sut: WooPosReceiptPrinterConnectionController
            whenever(receiptPrinterFacade.connect(PRINTER)).doSuspendableAnswer {
                sut.startSetup()
                ReceiptPrinterResult.Success
            }
            sut = createController()
            sut.startSetup()
            find(PRINTER)

            // WHEN
            sut.connect(PRINTER)
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Searching)
        }

    @Test
    fun `given setup is cancelled as soon as it is searching, when setup starts, then no search is started`() = runTest {
        // GIVEN
        val sut = createController()
        onEachStateInline(sut) { if (it is Searching) sut.cancelSetup() }

        // WHEN
        sut.startSetup()
        runCurrent()

        // THEN
        assertThat(sut.state.value).isEqualTo(Idle)
        assertThat(searchesStarted).isEqualTo(0)
    }

    @Test
    fun `given setup is cancelled as soon as it is connecting, when connecting, then no connection is started`() =
        runTest {
            // GIVEN
            val sut = createController()
            sut.startSetup()
            find(PRINTER)
            onEachStateInline(sut) { if (it is Connecting) sut.cancelSetup() }

            // WHEN
            sut.connect(PRINTER)
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Idle)
            verify(receiptPrinterFacade, never()).connect(PRINTER)
        }

    @Test
    fun `given setup restarts as soon as it is connecting, when setup is cancelled, then the restarted search stops`() =
        runTest {
            // GIVEN
            val sut = createController()
            sut.startSetup()
            find(PRINTER)
            onEachStateInline(sut) { if (it is Connecting) sut.startSetup() }
            sut.connect(PRINTER)
            runCurrent()

            // WHEN
            sut.cancelSetup()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Idle)
            assertThat(isDiscovering).isFalse()
            verify(receiptPrinterFacade, never()).connect(PRINTER)
        }

    @Test
    fun `given setup restarts as soon as the search fails, when the failed search ends, then the new search is kept`() =
        runTest {
            // GIVEN
            whenever(receiptPrinterFacade.discover()).thenReturn(
                flowOf(ReceiptPrinterDiscoveryEvent.Failed(ReceiptPrinterError.DiscoveryFailure)),
                discoveryEvents.receiveAsFlow(),
            )
            val sut = createController()
            onEachStateInline(sut) { if (it is DiscoveryFailed) sut.startSetup() }

            // WHEN
            sut.startSetup()
            runCurrent()

            // THEN
            assertThat(sut.state.value).isEqualTo(Searching)
        }

    @Test
    fun `given setup was cancelled, when it starts again, then the new search is not affected`() = runTest {
        // GIVEN
        val sut = createController()
        sut.startSetup()
        runCurrent()
        sut.cancelSetup()

        // WHEN
        sut.startSetup()
        find(PRINTER)

        // THEN
        assertThat(sut.state.value).isEqualTo(FoundOne(PRINTER))
        assertThat(isDiscovering).isTrue()
    }

    @Test
    fun `given a printer is connected, when reading the connection, then it is connected with the printer name`() =
        runTest {
            // GIVEN
            connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(PRINTER)
            val sut = createController()

            // THEN
            assertThat(sut.isConnected.first()).isTrue()
            assertThat(sut.connectedPrinterName.first()).isEqualTo(PRINTER.name)
        }

    @Test
    fun `given a printer is still connecting, when reading the connection, then it is not connected and has no name`() =
        runTest {
            // GIVEN
            connectionStatus.value = ReceiptPrinterConnectionStatus.Connecting(PRINTER)
            val sut = createController()

            // THEN
            assertThat(sut.isConnected.first()).isFalse()
            assertThat(sut.connectedPrinterName.first()).isNull()
        }

    private fun TestScope.createController() = WooPosReceiptPrinterConnectionController(
        receiptPrinterFacade = receiptPrinterFacade,
        permissionUtils = permissionUtils,
        scope = backgroundScope,
    )

    private fun TestScope.onEachStateInline(
        sut: WooPosReceiptPrinterConnectionController,
        action: (WooPosReceiptPrinterConnectionState) -> Unit,
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.state.collect { action(it) } }
    }

    private fun TestScope.find(device: ReceiptPrinterDevice) {
        discoveryEvents.trySend(ReceiptPrinterDiscoveryEvent.PrinterFound(device))
        runCurrent()
    }

    private companion object {
        val PRINTER = ReceiptPrinterDevice(id = "BT:00:11:22", name = "mC-Print3")
        val OTHER_PRINTER = ReceiptPrinterDevice(id = "BT:33:44:55", name = "TSP100")
        val THIRD_PRINTER = ReceiptPrinterDevice(id = "BT:66:77:88", name = "mPOP")
    }
}
