package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosScanningSetupState(
    val isVisible: Boolean = false,
    val currentStep: ScanningSetupStep,
    val selectedDevice: BarcodeReaderDevice? = null
) : Parcelable {
    enum class BarcodeReaderDevice(@StringRes val displayNameRes: Int) {
        TERA_1200(R.string.woopos_scanning_setup_device_tera_1200),
        STAR_BSH_20B(R.string.woopos_scanning_setup_device_star_bsh_20b),
        INATECK_BLUETOOTH(R.string.woopos_scanning_setup_device_inateck_bluetooth),
        OTHER(R.string.woopos_scanning_setup_device_other)
    }
    sealed class ScanningSetupStep : Parcelable {

        @Parcelize
        data class DeviceSelection(
            val title: String,
            val devices: List<BarcodeReaderDevice>,
        ) : ScanningSetupStep()

        @Parcelize
        data class Introduction(
            val title: String,
            val message: String,
            val primaryButtonText: String,
            val secondaryButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class BluetoothWarning(
            val title: String,
            val message: String,
            val primaryButtonText: String,
            val secondaryButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class BluetoothPairing(
            val title: String,
            val message: String,
            @DrawableRes val barcodeImageRes: Int,
            val instructionText: String,
            val primaryButtonText: String,
            val secondaryButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class PairOnYourDevice(
            val title: String,
            val message: String,
            @DrawableRes val barcodeImageRes: Int,
            val instructionText: String,
            val primaryButtonText: String,
            val secondaryButtonText: String,
            val bluetoothSettingsButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class TestYourScanner(
            val title: String,
            val message: String,
            @DrawableRes val barcodeImageRes: Int,
            val instructionText: String,
            val primaryButtonText: String,
            val secondaryButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerSetupComplete(
            val title: String,
            val message: String,
            val primaryButtonText: String,
        ) : ScanningSetupStep()
    }
}
