package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosScanningSetupState(
    val isVisible: Boolean = false,
    val currentStep: ScanningSetupStep
) : Parcelable {
    sealed class ScanningSetupStep : Parcelable {
        @Parcelize
        data class Welcome(
            val title: String,
            val message: String,
            val bluetoothOptionTitle: String,
            val bluetoothOptionDescription: String,
            val skipButtonText: String,
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
