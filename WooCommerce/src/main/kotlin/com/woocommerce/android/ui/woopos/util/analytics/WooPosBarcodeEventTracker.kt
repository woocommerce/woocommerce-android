package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.common.util.WooPosScannerDetectionUtil
import javax.inject.Inject

class WooPosBarcodeEventTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val scannerDetectionUtil: WooPosScannerDetectionUtil,
) {
    suspend fun trackBarcodeEvent(result: BarcodeInputDetector.BarcodeResult) {
        val connectedScanner = scannerDetectionUtil.detectConnectedScanner()
        val scannerInfo = scannerDetectionUtil.getScannerInfoString(connectedScanner)

        when (result) {
            is BarcodeInputDetector.BarcodeResult.Success -> {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.BarcodeScanned(
                        scanDurationMs = result.scanDurationMs,
                        barcodeLength = result.barcode.length,
                        scannerInfo = scannerInfo,
                    )
                )
            }

            is BarcodeInputDetector.BarcodeResult.Error -> {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.BarcodeScanningFailed(
                        scanDurationMs = result.scanDurationMs,
                        barcodeLength = result.barcode.length,
                        scannerInfo = scannerInfo,
                        failReason = result.failureReason.value,
                    )
                )
            }
        }
    }
}
