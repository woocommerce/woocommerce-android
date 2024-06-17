package com.woocommerce.android.ui.payments.cardreader.connect

import androidx.annotation.StringRes
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class CardReaderConnectEvent : MultiLiveEvent.Event() {
    data class CheckLocationPermissions(
        val onLocationPermissionsCheckResult: (permissionGranted: Boolean, showRationale: Boolean) -> Unit
    ) : CardReaderConnectEvent()

    data class CheckLocationEnabled(val onLocationEnabledCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

    data class CheckBluetoothEnabled(val onBluetoothCheckResult: (Boolean) -> Unit) : CardReaderConnectEvent()

    data class CheckBluetoothPermissionsGiven(
        val onBluetoothPermissionsGivenCheckResult: (Boolean) -> Unit
    ) : CardReaderConnectEvent()

    data class RequestEnableBluetooth(val onEnableBluetoothRequestResult: (Boolean) -> Unit) :
        CardReaderConnectEvent()

    data class RequestBluetoothRuntimePermissions(
        val onBluetoothRuntimePermissionsRequestResult: (Boolean) -> Unit
    ) : CardReaderConnectEvent()

    data class RequestLocationPermissions(val onPermissionsRequestResult: (Boolean) -> Unit) :
        CardReaderConnectEvent()

    object OpenPermissionsSettings : CardReaderConnectEvent()

    data class OpenLocationSettings(val onLocationSettingsClosed: () -> Unit) : CardReaderConnectEvent()

    data class ShowCardReaderTutorial(
        val cardReaderFlowParam: CardReaderFlowParam,
        val cardReaderType: CardReaderType,
    ) : CardReaderConnectEvent()

    object ShowUpdateInProgress : CardReaderConnectEvent()

    data class ShowToast(@StringRes val message: Int) : CardReaderConnectEvent()

    data class ShowToastString(val message: String) : CardReaderConnectEvent()

    data class OpenWPComWebView(val url: String) : CardReaderConnectEvent()

    data class OpenGenericWebView(val url: String) : CardReaderConnectEvent()

    data object PopBackStackForWooPOS : CardReaderConnectEvent()
}
