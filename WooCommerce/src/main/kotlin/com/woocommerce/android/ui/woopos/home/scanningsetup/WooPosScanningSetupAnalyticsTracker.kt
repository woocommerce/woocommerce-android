package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import javax.inject.Inject

class WooPosScanningSetupAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
) {
    suspend fun trackDialogShown() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.BarcodeScannerSetupFlowShown)
    }

    suspend fun trackScannerSelected(device: BarcodeReaderDevice) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupScannerSelected(
                scanner = device.toAnalyticsString()
            )
        )
    }

    suspend fun trackNextTapped(device: BarcodeReaderDevice, step: ScanningSetupStep) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupNextTapped(
                scanner = device.toAnalyticsString(),
                step = step.toAnalyticsString()
            )
        )
    }

    suspend fun trackBackTapped(device: BarcodeReaderDevice, step: ScanningSetupStep) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupBackTapped(
                scanner = device.toAnalyticsString(),
                step = step.toAnalyticsString()
            )
        )
    }

    suspend fun trackOpenSystemSettingsTapped(device: BarcodeReaderDevice) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupOpenSystemSettingsTapped(
                scanner = device.toAnalyticsString()
            )
        )
    }

    suspend fun trackTestScanSuccess(device: BarcodeReaderDevice) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupTestScanSuccess(
                scanner = device.toAnalyticsString()
            )
        )
    }

    suspend fun trackTestScanFailed(device: BarcodeReaderDevice, scanValue: String) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupTestScanFailed(
                scanner = device.toAnalyticsString(),
                scanValue = scanValue
            )
        )
    }

    suspend fun trackTestScanTimedOut(device: BarcodeReaderDevice) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupTestScanTimedOut(
                scanner = device.toAnalyticsString()
            )
        )
    }

    suspend fun trackDismissed(device: BarcodeReaderDevice?, step: ScanningSetupStep) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupDismissed(
                scanner = device?.toAnalyticsString(),
                step = step.toAnalyticsString()
            )
        )
    }

    suspend fun trackRetryTapped(device: BarcodeReaderDevice) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupRetryTapped(
                scanner = device.toAnalyticsString()
            )
        )
    }

    suspend fun trackScannerConnected(device: BarcodeReaderDevice?, scannerInfo: String) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.BarcodeScannerSetupScannerConnected(
                scanner = device?.toAnalyticsString() ?: "unknown",
                scannerInfo = scannerInfo
            )
        )
    }

    private fun BarcodeReaderDevice.toAnalyticsString(): String {
        return when (this) {
            BarcodeReaderDevice.TERA_1200 -> "tera_1200"
            BarcodeReaderDevice.STAR_BSH_20B -> "star_bsh_20b"
            BarcodeReaderDevice.NETUM_1228BC -> "netum_1228bc"
            BarcodeReaderDevice.OTHER -> "other"
        }
    }

    private fun ScanningSetupStep.toAnalyticsString(): String {
        return when (this) {
            is ScanningSetupStep.DeviceSelection -> "device_selection"
            is ScanningSetupStep.ScannerHIDModeSetup -> "setup_barcode_hid"
            is ScanningSetupStep.ScannerPairModeSetup -> "setup_barcode_pair"
            is ScanningSetupStep.ScannerSetupInfo -> "setup_information"
            is ScanningSetupStep.ScannerSetupBarcodesOnProducts -> "setup_products"
            is ScanningSetupStep.PairYourScanner -> "pairing"
            is ScanningSetupStep.TestYourScanner -> "test_barcode"
            is ScanningSetupStep.TestYourScannerScanFailed -> "test_scan_failed"
            is ScanningSetupStep.TestYourScannerTimeout -> "test_scan_timed_out"
            is ScanningSetupStep.ScannerSetupSuccess -> "setup_complete"
        }
    }
}
