package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.common.util.WooPosScannerDetectionUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WooPosBarcodeEventTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val scannerDetectionUtil: WooPosScannerDetectionUtil,
) {
    suspend fun trackBarcodeEvent(result: BarcodeInputDetector.BarcodeResult) {
        val connectedScanner = scannerDetectionUtil.detectConnectedScanner(context)
        val scannerInfo = scannerDetectionUtil.getScannerInfoString(connectedScanner)

        when (result) {
            is BarcodeInputDetector.BarcodeResult.Success -> {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.BarcodeScanned(
                        scanDurationMs = result.scanDurationMs,
                        isNumericOnly = result.barcode.all { it.isDigit() },
                        barcodeLength = result.barcode.length,
                        scannerInfo = scannerInfo,
                    )
                )
            }

            is BarcodeInputDetector.BarcodeResult.Error -> {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.BarcodeScanned(
                        scanDurationMs = result.scanDurationMs,
                        isNumericOnly = result.barcode.all { it.isDigit() },
                        barcodeLength = result.barcode.length,
                        scannerInfo = scannerInfo,
                        failReason = result.failureReason.value,
                    )
                )
            }
        }
    }
}
