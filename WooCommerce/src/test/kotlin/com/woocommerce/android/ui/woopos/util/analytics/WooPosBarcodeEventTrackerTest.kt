package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.common.util.WooPosScannerDetectionUtil
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WooPosBarcodeEventTrackerTest {
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val scannerDetectionUtil: WooPosScannerDetectionUtil = mock()
    private val context: Context = mock()

    private val tracker = WooPosBarcodeEventTracker(
        context = context,
        analyticsTracker = analyticsTracker,
        scannerDetectionUtil = scannerDetectionUtil
    )

    @Test
    fun `given success result, when tracking barcode event, then tracks success event with correct properties`() =
        runTest {
            // GIVEN
            val result = BarcodeInputDetector.BarcodeResult.Success(
                barcode = "1234567890",
                scanDurationMs = 150L
            )
            whenever(scannerDetectionUtil.detectConnectedScanner()).thenReturn(mock())
            whenever(scannerDetectionUtil.getScannerInfoString(any())).thenReturn("USB Scanner")

            // WHEN
            tracker.trackBarcodeEvent(result)

            // THEN
            verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.BarcodeScanned>())
        }

    @Test
    fun `given error result, when tracking barcode event, then tracks failure event with correct properties`() =
        runTest {
            // GIVEN
            val result = BarcodeInputDetector.BarcodeResult.Error(
                barcode = "123",
                scanDurationMs = 100L,
                failureReason = BarcodeInputDetector.FailureReason.TOO_SHORT,
            )
            whenever(scannerDetectionUtil.detectConnectedScanner()).thenReturn(mock())
            whenever(scannerDetectionUtil.getScannerInfoString(any())).thenReturn("USB Scanner")

            // WHEN
            tracker.trackBarcodeEvent(result)

            // THEN
            verify(analyticsTracker).track(any<WooPosAnalyticsEvent.Event.BarcodeScanningFailed>())
        }
}
