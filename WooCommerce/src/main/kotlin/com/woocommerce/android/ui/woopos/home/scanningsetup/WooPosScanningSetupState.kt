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
        abstract val previousStep: ScanningSetupStep?

        @Parcelize
        data class DeviceSelection(
            val title: String,
            val devices: List<BarcodeReaderDevice>,
            override val previousStep: ScanningSetupStep?
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerHIDModeSetup(
            val title: String,
            val message: String,
            @DrawableRes val qrCodeImageRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String,
            override val previousStep: ScanningSetupStep
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerPairModeSetup(
            val title: String,
            val message: String,
            @DrawableRes val qrCodeImageRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String,
            override val previousStep: ScanningSetupStep
        ) : ScanningSetupStep()

        @Parcelize
        data class PairYourScanner(
            val title: String,
            val message: String,
            @DrawableRes val iconRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String,
            val bluetoothSettingsButtonText: String,
            override val previousStep: ScanningSetupStep
        ) : ScanningSetupStep()

        @Parcelize
        data class TestYourScanner(
            val title: String,
            val message: String,
            @DrawableRes val barcodeImageRes: Int,
            val secondaryButtonText: String,
            override val previousStep: ScanningSetupStep
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerSetupSuccess(
            val title: String,
            val message: String,
            val moreInfoButtonText: String,
            override val previousStep: ScanningSetupStep?
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerSetupInfo(
            val title: String,
            val message: String,
            val bulletPoints: List<String>,
            val infoText: String,
            val backButtonText: String,
            val doneButtonText: String,
            override val previousStep: ScanningSetupStep?
        ) : ScanningSetupStep()
    }
}
