package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosAnalyticsTrackingDataKeeper @Inject constructor() {
    var interactionWithCustomerStartedTimestamp: Long? = null
    var orderSyncSuccessTimestamp: Long? = null
    var readerReadyForPaymentTimestamp: Long? = null

    // Scoped to the current payment attempt, so a retry never reports the previous attempt's preparation.
    var readerReadyForAttemptTimestamp: Long? = null
    var paymentAttemptStartedTimestamp: Long? = null
        private set
    var readerReadySignal: ReaderReadySignal? = null
    var cardTappedTimestamp: Long? = null
    var checkoutButtonTapsCount: Int = 0

    // Starts at the later of order sync and the start of this attempt, so time on a previous attempt's error screen
    // does not count as preparation.
    val millisecondsFromOrderSyncToReaderReady: Long?
        get() {
            val orderSync = orderSyncSuccessTimestamp ?: return null
            val readerReady = readerReadyForAttemptTimestamp ?: return null
            val preparationStart = maxOf(orderSync, paymentAttemptStartedTimestamp ?: orderSync)
            return (readerReady - preparationStart).takeIf { it >= 0 }
        }

    val readerReadyTimingProperties: Map<String, String>
        get() = buildMap {
            millisecondsFromOrderSyncToReaderReady?.let {
                put(MILLISECONDS_FROM_ORDER_SYNC_TO_READER_READY, it.toString())
            }
            readerReadySignal?.let { put(ReaderReadySignal.READER_READY_SIGNAL, it.value) }
        }

    fun startPaymentAttempt(timestamp: Long) {
        readerReadyForAttemptTimestamp = null
        readerReadySignal = null
        paymentAttemptStartedTimestamp = timestamp
    }

    fun reset() {
        interactionWithCustomerStartedTimestamp = null
        orderSyncSuccessTimestamp = null
        readerReadyForPaymentTimestamp = null
        readerReadyForAttemptTimestamp = null
        paymentAttemptStartedTimestamp = null
        readerReadySignal = null
        cardTappedTimestamp = null
        checkoutButtonTapsCount = 0
    }

    private companion object {
        const val MILLISECONDS_FROM_ORDER_SYNC_TO_READER_READY = "milliseconds_from_order_sync_to_reader_ready"
    }
}
