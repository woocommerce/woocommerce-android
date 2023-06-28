package com.woocommerce.android.ui.barcodescanner

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_FAILURE_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.ScanningSource
import javax.inject.Inject

class BarcodeScanningTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackScanFailure(source: ScanningSource, type: CodeScanningErrorType) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BARCODE_SCANNING_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to source.source,
                KEY_SCANNING_FAILURE_REASON to type.toString(),
            )
        )
    }

    fun trackSuccess(source: ScanningSource) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BARCODE_SCANNING_SUCCESS,
            mapOf(
                KEY_SCANNING_SOURCE to source.source
            )
        )
    }
}
