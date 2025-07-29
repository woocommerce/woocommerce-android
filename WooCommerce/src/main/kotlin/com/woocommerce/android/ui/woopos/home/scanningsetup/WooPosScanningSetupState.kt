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
    val selectedDevice: BarcodeReaderDevice? = null,
    val wasDialogShown: Boolean = false,
) : Parcelable {
    enum class BarcodeReaderDevice(@StringRes val displayNameRes: Int) {
        STAR_BSH_20B(R.string.woopos_scanning_setup_device_star_bsh_20b),
        TERA_1200(R.string.woopos_scanning_setup_device_tera_1200),
        NETUM_1228BC(R.string.woopos_scanning_setup_device_netum_1228bc),
        OTHER(R.string.woopos_scanning_setup_device_other)
    }

    object ScannerConfigurations {
        fun getStepSequence(device: BarcodeReaderDevice): List<ScanningSetupStep> {
            return when (device) {
                BarcodeReaderDevice.TERA_1200 -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_hid_tera_1200
                    ),
                    ScanningSetupStep.ScannerPairModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_pairing_tera_1200
                    ),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.SoftwareKeyboardSetup,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.STAR_BSH_20B -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_star_bsh_20
                    ),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.SoftwareKeyboardSetup,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.NETUM_1228BC -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerHIDModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_hid_netum_1228
                    ),
                    ScanningSetupStep.ScannerPairModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_pairing_netum_1228
                    ),
                    ScanningSetupStep.PairYourScanner(deviceName = device.displayNameRes),
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.SoftwareKeyboardSetup,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )

                BarcodeReaderDevice.OTHER -> listOf(
                    ScanningSetupStep.DeviceSelection,
                    ScanningSetupStep.ScannerSetupInfo,
                    ScanningSetupStep.TestYourScanner,
                    ScanningSetupStep.SoftwareKeyboardSetup,
                    ScanningSetupStep.ScannerSetupSuccess,
                    ScanningSetupStep.ScannerSetupBarcodesOnProducts,
                )
            }
        }
    }

    sealed class ScanningSetupStep : Parcelable {
        @Parcelize
        data object DeviceSelection : ScanningSetupStep() {
            @IgnoredOnParcel
            val devices: List<BarcodeReaderDevice> = listOf(
                BarcodeReaderDevice.STAR_BSH_20B,
                BarcodeReaderDevice.TERA_1200,
                BarcodeReaderDevice.NETUM_1228BC,
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
            val titleRes = R.string.woopos_scanning_setup_hid_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_hid_message

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_next

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
        }

        @Parcelize
        data class ScannerPairModeSetup(
            @DrawableRes val qrCodeImageRes: Int
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
        data object SoftwareKeyboardSetup : ScanningSetupStep() {
            @get:StringRes
            @IgnoredOnParcel
            val titleRes = R.string.woopos_scanning_setup_software_keyboard_title

            @get:StringRes
            @IgnoredOnParcel
            val messageRes = R.string.woopos_scanning_setup_software_keyboard_message

            @get:StringRes
            @IgnoredOnParcel
            val hintRes = R.string.woopos_scanning_setup_software_keyboard_hint

            @get:StringRes
            @IgnoredOnParcel
            val primaryButtonTextRes = R.string.woopos_scanning_setup_button_next

            @get:StringRes
            @IgnoredOnParcel
            val secondaryButtonTextRes = R.string.woopos_scanning_setup_button_back
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
