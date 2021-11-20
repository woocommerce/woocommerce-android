package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.StringRes
import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class CardReaderConnectEvent : MultiLiveEvent.Event() {
    data class CheckRequiredPermissions(val onPermissionsCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

    data class CheckLocationEnabled(val onLocationEnabledCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

    data class CheckBluetoothEnabled(val onBluetoothCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

    data class RequestEnableBluetooth(val onEnableBluetoothRequestResult: (Boolean) -> Unit) :
        CardReaderConnectEvent()

    data class RequestRequiredPermissions(val onPermissionsRequestResult: (Map<String, Boolean>) -> Unit) :
        CardReaderConnectEvent()

    object OpenPermissionsSettings : CardReaderConnectEvent()

    data class OpenLocationSettings(val onLocationSettingsClosed: () -> Unit) : CardReaderConnectEvent()

    object ShowCardReaderTutorial : CardReaderConnectEvent()

    object ShowUpdateInProgress : CardReaderConnectEvent()

    object NavigateToOnboardingFlow : CardReaderConnectEvent()

    data class ShowToast(@StringRes val message: Int) : CardReaderConnectEvent()

    data class OpenWPComWebView(val url: String) : CardReaderConnectEvent()

    data class OpenGenericWebView(val url: String) : CardReaderConnectEvent()
}
