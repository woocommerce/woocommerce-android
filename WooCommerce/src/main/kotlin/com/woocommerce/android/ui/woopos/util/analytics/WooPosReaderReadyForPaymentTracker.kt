package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import javax.inject.Inject

class WooPosReaderReadyForPaymentTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
    private val trackingInfoProvider: CardReaderTrackingInfoProvider,
) {
    suspend fun track(signal: ReaderReadySignal) {
        val readerReadyTimestamp = System.currentTimeMillis()
        analyticsData.readerReadyForPaymentTimestamp = readerReadyTimestamp
        analyticsData.readerReadySignal = signal

        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment(
                waitingTimeSeconds = analyticsData.orderSyncSuccessTimestamp?.let { orderSyncTimestamp ->
                    (readerReadyTimestamp - orderSyncTimestamp) / MILLIS_IN_SECOND
                },
                transport = trackingInfoProvider.trackingInfo.transport,
                readerReadyTiming = analyticsData.readerReadyTimingProperties,
            )
        )
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1_000.0
    }
}
