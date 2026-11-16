package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
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
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WooPosReceiptPrinterConnectionController @AssistedInject constructor(
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade,
    private val permissionUtils: WooPosPermissionUtils,
    @Assisted private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<WooPosReceiptPrinterConnectionState>(Idle)
    val state: StateFlow<WooPosReceiptPrinterConnectionState> = _state.asStateFlow()

    val isConnected: Flow<Boolean> = receiptPrinterFacade.connectionStatus
        .map { it is ReceiptPrinterConnectionStatus.Connected }

    val connectedPrinterName: Flow<String?> = receiptPrinterFacade.connectionStatus
        .map { (it as? ReceiptPrinterConnectionStatus.Connected)?.device?.name }

    private var setupJob: Job? = null
    private var foundDevices = emptyList<ReceiptPrinterDevice>()
    private var excludedDeviceIds = emptySet<String>()
    private var isBluetoothPermissionPermanentlyDenied = false

    fun startSetup() {
        excludedDeviceIds = emptySet()
        startDiscovery()
    }

    fun onBluetoothPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (_state.value !is MissingBluetoothPermission) return
        isBluetoothPermissionPermanentlyDenied = !granted && !shouldShowRationale
        startSetup()
    }

    fun recheckBluetoothRequirement() {
        if (_state.value is MissingBluetoothPermission || _state.value is BluetoothOff) startSetup()
    }

    fun keepSearching() {
        val found = _state.value as? FoundOne ?: return
        excludedDeviceIds += found.device.id
        if (setupJob?.isActive == true) {
            foundDevices = emptyList()
            _state.value = Searching
        } else {
            startDiscovery()
        }
    }

    fun connect(device: ReceiptPrinterDevice) {
        val discoveryJob = setupJob?.apply { cancel() }
        _state.value = Connecting(device)
        setupJob = scope.launch {
            discoveryJob?.join()
            _state.value = when (receiptPrinterFacade.connect(device)) {
                ReceiptPrinterResult.Success -> Idle
                is ReceiptPrinterResult.Failure -> ConnectionFailed
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
        foundDevices = emptyList()
        _state.value = Searching
        setupJob = scope.launch {
            receiptPrinterFacade.discover().collect { event ->
                when (event) {
                    is ReceiptPrinterDiscoveryEvent.PrinterFound -> onPrinterFound(event.device)
                    is ReceiptPrinterDiscoveryEvent.Failed -> _state.value = DiscoveryFailed
                }
            }
            if (_state.value is Searching) _state.value = NoPrintersFound
        }
    }

    private fun unmetBluetoothRequirement(): WooPosReceiptPrinterConnectionState? = when {
        !permissionUtils.hasBluetoothScanAndConnectPermissions() ->
            MissingBluetoothPermission(permanentlyDenied = isBluetoothPermissionPermanentlyDenied)

        !permissionUtils.isBluetoothEnabled() -> BluetoothOff

        else -> null
    }

    private fun onPrinterFound(device: ReceiptPrinterDevice) {
        if (device.id in excludedDeviceIds || foundDevices.any { it.id == device.id }) return
        foundDevices = foundDevices + device
        _state.value = foundDevices.singleOrNull()?.let { FoundOne(it) } ?: FoundMultiple(foundDevices)
    }

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): WooPosReceiptPrinterConnectionController
    }
}
