package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosScanningSetupState(
    val isVisible: Boolean = false,
    val currentStep: ScanningSetupStep,
    val selectedDevice: BarcodeReaderDevice? = null
) : Parcelable {
    enum class BarcodeReaderDevice(val displayName: String) {
        TERA_1200("Tera 1200"),
        STAR_BSH_20B("Star BSH-20B"),
        INATECK_BLUETOOTH("Inateck Bluetooth")
    }
    sealed class ScanningSetupStep : Parcelable {
        @Parcelize
        data class Welcome(
            val title: String,
            val message: String,
            val setupButtonText: String,
            val documentationButtonText: String,
        ) : ScanningSetupStep()

        @Parcelize
        data class DeviceSelection(
            val title: String,
            val devices: List<BarcodeReaderDevice>,
            val primaryButtonText: String,
            val secondaryButtonText: String,
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
