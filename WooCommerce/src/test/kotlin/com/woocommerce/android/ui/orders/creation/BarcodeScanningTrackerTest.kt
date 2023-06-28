package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.barcodescanner.BarcodeScanningTracker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class BarcodeScanningTrackerTest : BaseUnitTest() {
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var barcodeScanningTracker: BarcodeScanningTracker

    @Before
    fun setup() {
        analyticsTrackerWrapper = mock()
        barcodeScanningTracker = BarcodeScanningTracker(analyticsTrackerWrapper)
    }

    @Test
    fun `when scan failure, then track barcode scanning failure`() {
        barcodeScanningTracker.trackScanFailure(ScanningSource.ORDER_LIST, CodeScanningErrorType.NotFound)

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.BARCODE_SCANNING_FAILURE),
            any()
        )
    }

    @Test
    fun `when scan failure, then track barcode scanning failure with correct source and type`() {
        barcodeScanningTracker.trackScanFailure(ScanningSource.ORDER_LIST, CodeScanningErrorType.NotFound)

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.BARCODE_SCANNING_FAILURE,
            mapOf(
                AnalyticsTracker.KEY_SCANNING_SOURCE to ScanningSource.ORDER_LIST.source,
                AnalyticsTracker.KEY_SCANNING_FAILURE_REASON to CodeScanningErrorType.NotFound.toString(),
            )
        )
    }

    @Test
    fun `when scan success, then track barcode scanning success`() {
        barcodeScanningTracker.trackSuccess(ScanningSource.ORDER_LIST)

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.BARCODE_SCANNING_SUCCESS),
            any()
        )
    }
}
