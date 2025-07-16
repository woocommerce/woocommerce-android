package com.woocommerce.android.ui.woopos.home.scanningsetup

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosScanningSetupState(
    val currentStep: ScanningSetupStep,
    val selectedDevice: BarcodeReaderDevice? = null
) : Parcelable {
    enum class BarcodeReaderDevice(@StringRes val displayNameRes: Int) {
        TERA_1200(R.string.woopos_scanning_setup_device_tera_1200),
        STAR_BSH_20B(R.string.woopos_scanning_setup_device_star_bsh_20b),
        INATECK_BLUETOOTH(R.string.woopos_scanning_setup_device_inateck_bluetooth),
        OTHER(R.string.woopos_scanning_setup_device_other)
    }

    object ScannerConfigurations {
        fun getStepSequence(device: BarcodeReaderDevice): List<ScanningSetupStep> {
            return when (device) {
                BarcodeReaderDevice.TERA_1200 -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode),
                    ScanningSetupStep.ScannerPairModeSetup(),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.STAR_BSH_20B -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.INATECK_BLUETOOTH -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode),
                    ScanningSetupStep.ScannerPairModeSetup(),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.OTHER -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerSetupInfo,
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.ScannerSetupSuccess
                )
            }
        }
    }

    sealed class ScanningSetupStep : Parcelable {
        @Parcelize
        data object DeviceSelection : ScanningSetupStep() {
            @IgnoredOnParcel
            val devices: List<BarcodeReaderDevice> = listOf(
                BarcodeReaderDevice.TERA_1200,
                BarcodeReaderDevice.STAR_BSH_20B,
                BarcodeReaderDevice.INATECK_BLUETOOTH,
                BarcodeReaderDevice.OTHER
            )

            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_device_selection_title
        }

        @Parcelize
        data class ScannerHIDModeSetup(
            @DrawableRes val qrCodeImageRes: Int
        ) : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_introduction_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_introduction_message

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_next

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data class ScannerPairModeSetup(
            @DrawableRes val qrCodeImageRes: Int = R.drawable.ic_barcode
        ) : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_scanner_pair_mode_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_scanner_pair_mode_message

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_next

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data class PairYourScanner(@StringRes val deviceName: Int) : ScanningSetupStep() {
            @get:DrawableRes
            @IgnoredOnParcel
            val iconRes: Int = R.drawable.ic_woopos_bluetooth_settings

            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_pair_your_scanner_title

            @IgnoredOnParcel
            val messageRes =
                UiString.UiStringRes(
                    R.string.woopos_scanning_setup_pair_your_scanner_message,
                    listOf(
                        UiString.UiStringRes(
                            deviceName
                        )
                    )
                )

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_next

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back

            @get:StringRes
            @IgnoredOnParcel
            val bluetoothSettingsButtonTextRes = R.string.woopos_scanning_setup_go_to_settings
        }

        @Parcelize
        data object TestYourScanner : ScanningSetupStep() {
            @IgnoredOnParcel val barcodeValue: String = TEST_BARCODE_EAN13

            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_test_scanner_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_test_scanner_message

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data object TestYourScannerTimeout : ScanningSetupStep() {
            @IgnoredOnParcel val barcodeValue: String = TEST_BARCODE_EAN13

            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_timeout_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_timeout_message

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data object TestYourScannerScanFailed : ScanningSetupStep() {
            @get:DrawableRes
            @IgnoredOnParcel
            val iconRes: Int = R.drawable.ic_woo_pos_error_x

            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_scan_failed_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_scan_failed_message

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_retry

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data object ScannerSetupSuccess : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_success_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_success_message

            @get:StringRes
            @IgnoredOnParcel
            val moreInfoButtonTextRes = R.string.woopos_scanning_setup_more_information
        }

        @Parcelize
        data object ScannerSetupInfo : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_info_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_info_message

            @get:StringRes
            @IgnoredOnParcel
            val infoTextRes = R.string.woopos_scanning_setup_info_text

            @get:StringRes
            @IgnoredOnParcel
            val backButtonTextRes = R.string.woopos_scanning_setup_button_back

            @get:StringRes
            @IgnoredOnParcel
            val nextButtonTextRes = R.string.woopos_scanning_setup_button_next

            @IgnoredOnParcel
            val bulletPointsRes = listOf(
                R.string.woopos_scanning_setup_info_bullet_1,
                R.string.woopos_scanning_setup_info_bullet_2,
                R.string.woopos_scanning_setup_info_bullet_3
            )
        }

        @Parcelize
        data object ScannerSetupBarcodesOnProducts : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_barcodes_on_products_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_barcodes_on_products_message

            @get:StringRes
            @IgnoredOnParcel
            val backButtonTextRes = R.string.woopos_scanning_setup_button_back

            @get:StringRes
            @IgnoredOnParcel
            val doneButtonTextRes = R.string.woopos_scanning_setup_button_done
        }
    }

    companion object {
        const val TEST_BARCODE_EAN13 = "1234567890128"
    }
}
