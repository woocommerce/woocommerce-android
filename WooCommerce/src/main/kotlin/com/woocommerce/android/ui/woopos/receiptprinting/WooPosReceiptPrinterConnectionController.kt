package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.BluetoothOff
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Connected
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Connecting
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.ConnectionFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.DiscoveryFailed
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundMultiple
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.FoundOne
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Idle
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.MissingBluetoothPermission
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.NoPrintersFound
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterConnectionState.Searching
import com.woocommerce.android.ui.woopos.util.WooPosBluetoothStatus
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WooPosReceiptPrinterConnectionController @AssistedInject constructor(
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade,
    private val permissionUtils: WooPosPermissionUtils,
    private val bluetoothStatus: WooPosBluetoothStatus,
    @Assisted private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<WooPosReceiptPrinterConnectionState>(Idle)
    val state: StateFlow<WooPosReceiptPrinterConnectionState> = _state.asStateFlow()

    private var setupJob: Job? = null
    private var excludedDeviceIds = emptySet<String>()

    fun startSetup() {
        excludedDeviceIds = emptySet()
        startDiscovery()
    }

    fun onBluetoothPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (_state.value !is MissingBluetoothPermission) return
        if (granted) {
            startSetup()
        } else {
            _state.value = MissingBluetoothPermission(permanentlyDenied = !shouldShowRationale)
        }
    }

    fun recheckBluetoothRequirement() {
        if (_state.value is MissingBluetoothPermission || _state.value is BluetoothOff) startSetup()
    }

    fun keepSearching() {
        val found = _state.value as? FoundOne ?: return
        excludedDeviceIds += found.device.id
        if (setupJob?.isActive == true) {
            _state.value = Searching
        } else {
            startDiscovery()
        }
    }

    fun connect(device: ReceiptPrinterDevice) {
        val previousJob = setupJob?.apply { cancel() }
        launchSetup(Connecting(device)) {
            previousJob?.join()
            val result = receiptPrinterFacade.connect(device)
            ensureActive()
            _state.value = when (result) {
                ReceiptPrinterResult.Success -> Connected(device)
                is ReceiptPrinterResult.Failure -> ConnectionFailed(device, result.error)
            }
        }
    }

    fun cancelSetup() {
        setupJob?.cancel()
        _state.value = Idle
    }

    private fun startDiscovery() {
        setupJob?.cancel()
        val unmetRequirement = unmetBluetoothRequirement()
        if (unmetRequirement != null) {
            _state.value = unmetRequirement
            return
        }
        launchSetup(Searching) {
            receiptPrinterFacade.discover().collect { event ->
                when (event) {
                    is ReceiptPrinterDiscoveryEvent.PrinterFound -> onPrinterFound(event.device)
                    is ReceiptPrinterDiscoveryEvent.Failed ->
                        if (_state.value is Searching) _state.value = DiscoveryFailed
                }
            }
            ensureActive()
            if (_state.value is Searching) _state.value = NoPrintersFound
        }
    }

    // A state collector can call back into the controller while the state is published, so the job is registered
    // first: whatever that collector cancels or replaces is then this setup and not the previous one.
    private fun launchSetup(
        initialState: WooPosReceiptPrinterConnectionState,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val job = scope.launch(start = CoroutineStart.LAZY, block = block)
        setupJob = job
        _state.value = initialState
        job.start()
    }

    private fun unmetBluetoothRequirement(): WooPosReceiptPrinterConnectionState? = when {
        !permissionUtils.hasBluetoothScanAndConnectPermissions() ->
            _state.value as? MissingBluetoothPermission ?: MissingBluetoothPermission(permanentlyDenied = false)

        !bluetoothStatus.isEnabled() -> BluetoothOff

        else -> null
    }

    private fun onPrinterFound(device: ReceiptPrinterDevice) {
        val foundDevices = when (val current = _state.value) {
            is FoundOne -> listOf(current.device)
            is FoundMultiple -> current.devices
            else -> emptyList()
        }
        if (device.id in excludedDeviceIds || foundDevices.any { it.id == device.id }) return
        _state.value = if (foundDevices.isEmpty()) FoundOne(device) else FoundMultiple(foundDevices + device)
    }

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): WooPosReceiptPrinterConnectionController
    }
}
