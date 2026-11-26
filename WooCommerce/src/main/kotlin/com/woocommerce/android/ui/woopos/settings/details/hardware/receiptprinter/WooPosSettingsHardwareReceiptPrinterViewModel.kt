package com.woocommerce.android.ui.woopos.settings.details.hardware.receiptprinter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterFacade
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareReceiptPrinterViewModel @Inject constructor(
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {

    val uiState: StateFlow<WooPosSettingsHardwareReceiptPrinterUiState> = receiptPrinterFacade.connectionStatus
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = receiptPrinterFacade.connectionStatus.value.toUiState(),
        )

    fun onConnectClicked() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.SettingsEvent.ShowReceiptPrinterSetupDialog)
        }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.ReceiptPrinterDisconnectTapped)
            receiptPrinterFacade.disconnect()
        }
    }

    private fun ReceiptPrinterConnectionStatus.toUiState(): WooPosSettingsHardwareReceiptPrinterUiState =
        when (this) {
            is ReceiptPrinterConnectionStatus.Connected ->
                WooPosSettingsHardwareReceiptPrinterUiState.Connected(printerName = device.name)

            is ReceiptPrinterConnectionStatus.Connecting,
            ReceiptPrinterConnectionStatus.Disconnecting,
            ReceiptPrinterConnectionStatus.Disconnected -> WooPosSettingsHardwareReceiptPrinterUiState.Disconnected
        }
}
