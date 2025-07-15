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

    enum class ScannerStepType {
        HID_MODE_SETUP,
        PAIR_MODE_SETUP,
        PAIR_YOUR_SCANNER,
        TEST_YOUR_SCANNER,
        SETUP_SUCCESS,
        SETUP_INFO
    }

    data class ScannerConfiguration(
        val device: BarcodeReaderDevice,
        val stepSequence: List<ScannerStepType>
    ) {
        companion object {
            val configurations = mapOf(
                BarcodeReaderDevice.TERA_1200 to ScannerConfiguration(
                    device = BarcodeReaderDevice.TERA_1200,
                    stepSequence = listOf(
                        ScannerStepType.HID_MODE_SETUP,
                        ScannerStepType.PAIR_MODE_SETUP,
                        ScannerStepType.PAIR_YOUR_SCANNER,
                        ScannerStepType.TEST_YOUR_SCANNER,
                        ScannerStepType.SETUP_SUCCESS
                    )
                ),
                BarcodeReaderDevice.STAR_BSH_20B to ScannerConfiguration(
                    device = BarcodeReaderDevice.STAR_BSH_20B,
                    stepSequence = listOf(
                        ScannerStepType.HID_MODE_SETUP,
                        ScannerStepType.PAIR_YOUR_SCANNER,
                        ScannerStepType.TEST_YOUR_SCANNER,
                        ScannerStepType.SETUP_SUCCESS
                    )
                ),
                BarcodeReaderDevice.INATECK_BLUETOOTH to ScannerConfiguration(
                    device = BarcodeReaderDevice.INATECK_BLUETOOTH,
                    stepSequence = listOf(
                        ScannerStepType.HID_MODE_SETUP,
                        ScannerStepType.PAIR_MODE_SETUP,
                        ScannerStepType.PAIR_YOUR_SCANNER,
                        ScannerStepType.TEST_YOUR_SCANNER,
                        ScannerStepType.SETUP_SUCCESS
                    )
                ),
                BarcodeReaderDevice.OTHER to ScannerConfiguration(
                    device = BarcodeReaderDevice.OTHER,
                    stepSequence = listOf(
                        ScannerStepType.SETUP_INFO
                    )
                )
            )

            fun getConfiguration(device: BarcodeReaderDevice): ScannerConfiguration {
                return configurations[device] ?: error("No configuration found for device: $device")
            }
        }
    }
    sealed class ScanningSetupStep : Parcelable {
        @Parcelize
        data class DeviceSelection(
            val title: String,
            val devices: List<BarcodeReaderDevice>
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerHIDModeSetup(
            val title: String,
            val message: String,
            @DrawableRes val qrCodeImageRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerPairModeSetup(
            val title: String,
            val message: String,
            @DrawableRes val qrCodeImageRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class PairYourScanner(
            val title: String,
            val message: String,
            @DrawableRes val iconRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String,
            val bluetoothSettingsButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class TestYourScanner(
            val title: String,
            val message: String,
            val barcodeValue: String,
            val secondaryButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class TestYourScannerTimeout(
            val title: String,
            val message: String,
            val barcodeValue: String,
            val secondaryButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class TestYourScannerScanFailed(
            val title: String,
            val message: String,
            @DrawableRes val iconRes: Int,
            val primaryButtonText: String,
            val secondaryButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerSetupSuccess(
            val title: String,
            val message: String,
            val moreInfoButtonText: String
        ) : ScanningSetupStep()

        @Parcelize
        data class ScannerSetupInfo(
            val title: String,
            val message: String,
            val bulletPoints: List<String>,
            val infoText: String,
            val backButtonText: String,
            val doneButtonText: String
        ) : ScanningSetupStep()
    }
}
