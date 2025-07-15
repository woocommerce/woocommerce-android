package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice

sealed class WooPosScanningSetupUiEvent {
    data object OnPrimaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnSecondaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnOpenBluetoothSettings : WooPosScanningSetupUiEvent()
    data class OnDeviceSelected(val device: BarcodeReaderDevice) : WooPosScanningSetupUiEvent()
}
