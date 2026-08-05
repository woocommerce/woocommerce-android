package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoProvider
import javax.inject.Inject

/**
 * Tracks `reader_ready_for_card_payment`, which is dispatched through [WooPosAnalyticsTracker] and so never
 * passes through PaymentsFlowTracker. The transport is read from the shared card reader tracking info so
 * remote (wifi_lan) sessions are distinguishable from local ones.
 */
class WooPosReaderReadyForPaymentTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
    private val trackingInfoProvider: CardReaderTrackingInfoProvider,
) {
    suspend fun track() {
        val readerReadyTimestamp = System.currentTimeMillis()
        analyticsData.readerReadyForPaymentTimestamp = readerReadyTimestamp

        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment(
                waitingTimeSeconds = analyticsData.orderSyncSuccessTimestamp?.let { orderSyncTimestamp ->
                    (readerReadyTimestamp - orderSyncTimestamp) / MILLIS_IN_SECOND
                },
                transport = trackingInfoProvider.trackingInfo.transport,
            )
        )
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1_000
    }
}
