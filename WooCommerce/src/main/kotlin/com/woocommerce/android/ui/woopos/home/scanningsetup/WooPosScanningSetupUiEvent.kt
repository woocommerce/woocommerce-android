package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice

sealed class WooPosScanningSetupUiEvent {
    data object OnDialogShown : WooPosScanningSetupUiEvent()
    data object OnPrimaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnSecondaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnOpenBluetoothSettings : WooPosScanningSetupUiEvent()
    data object OnDismissed : WooPosScanningSetupUiEvent()
    data class OnDeviceSelected(val device: BarcodeReaderDevice) : WooPosScanningSetupUiEvent()
    data class OnBarcodeScanned(val barcodeResult: BarcodeInputDetector.BarcodeResult) : WooPosScanningSetupUiEvent()
}
