package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReceiptPrinterSetupSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosReceiptPrinterSetupViewModel @Inject constructor(
    controllerFactory: WooPosReceiptPrinterConnectionController.Factory,
    private val permissionUtils: WooPosPermissionUtils,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val controller = controllerFactory.create(viewModelScope)

    val state: StateFlow<WooPosReceiptPrinterConnectionState> = controller.state

    // The dialog calls this again when the Activity is recreated (for example when a Bluetooth scanner connects),
    // so a setup that is already running is left alone instead of being restarted.
    fun startSetup(source: ReceiptPrinterSetupSource) {
        if (state.value !is WooPosReceiptPrinterConnectionState.Idle) return
        track(WooPosAnalyticsEvent.Event.ReceiptPrinterSetupStarted(source))
        controller.startSetup()
    }

    fun dismissDialog() {
        controller.cancelSetup()
    }

    fun onConnected(source: ReceiptPrinterSetupSource) {
        track(WooPosAnalyticsEvent.Event.ReceiptPrinterConnected(source))
    }

    fun onResume() {
        controller.recheckBluetoothRequirement()
    }

    fun onBluetoothPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        controller.onBluetoothPermissionResult(granted, shouldShowRationale)
    }

    fun onBluetoothEnabled() {
        controller.recheckBluetoothRequirement()
    }

    fun onOpenAppSettingsClicked() {
        permissionUtils.showAppSettings()
    }

    fun onOpenBluetoothSettingsClicked() {
        permissionUtils.showBluetoothSettings()
    }

    fun onConnectClicked(device: ReceiptPrinterDevice) {
        controller.connect(device)
    }

    fun onKeepSearchingClicked() {
        controller.keepSearching()
    }

    fun onSearchAgainClicked() {
        controller.startSetup()
    }

    private fun track(event: WooPosAnalyticsEvent) {
        viewModelScope.launch { analyticsTracker.track(event) }
    }
}
