package com.woocommerce.android.ui.woopos.util.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosAnalyticsTrackingDataKeeper @Inject constructor(private val elapsedTime: WooPosElapsedTime) {
    var interactionWithCustomerStartedTimestamp: Long? = null
    var orderSyncSuccessTimestamp: Long? = null
    var readerReadyForPaymentTimestamp: Long? = null
    var cardTappedTimestamp: Long? = null
    var checkoutButtonTapsCount: Int = 0

    private var checkoutStartedTimestamp: Long? = null
    private var orderSyncCompletedTimestamp: Long? = null
    private var paymentConfirmedTimestamp: Long? = null
    private var syncStrategy = "unknown"
    private var checkoutGeneration = 0L

    @Synchronized
    fun startCheckoutTiming(strategy: String): Long {
        checkoutGeneration++
        checkoutStartedTimestamp = elapsedTime.milliseconds()
        orderSyncCompletedTimestamp = null
        paymentConfirmedTimestamp = null
        syncStrategy = strategy
        return checkoutGeneration
    }

    @Synchronized
    fun orderSyncCompleted(generation: Long) {
        if (
            generation == checkoutGeneration && checkoutStartedTimestamp != null && orderSyncCompletedTimestamp == null
        ) {
            orderSyncCompletedTimestamp = elapsedTime.milliseconds()
        }
    }

    @Synchronized
    fun paymentConfirmed() {
        if (checkoutStartedTimestamp != null && paymentConfirmedTimestamp == null) {
            paymentConfirmedTimestamp = elapsedTime.milliseconds()
        }
    }

    @Synchronized
    fun cardPaymentTiming(): Map<String, Any> = timingProperties(includePaymentConfirmed = true).also {
        paymentConfirmedTimestamp = null
    }

    @Synchronized
    fun cashPaymentTiming(): Map<String, Any> = timingProperties(includePaymentConfirmed = false)

    @Synchronized
    fun clearPaymentConfirmation() {
        paymentConfirmedTimestamp = null
    }

    private fun timingProperties(includePaymentConfirmed: Boolean): Map<String, Any> {
        val properties = mutableMapOf<String, Any>("sync_strategy" to syncStrategy)
        if (checkoutStartedTimestamp == null) return properties
        val now = elapsedTime.milliseconds()
        fun add(key: String, start: Long?) {
            if (start != null && now >= start) properties[key] = now - start
        }
        add("pos_time_since_checkout_started_ms", checkoutStartedTimestamp)
        add("pos_time_since_order_sync_completed_ms", orderSyncCompletedTimestamp)
        if (includePaymentConfirmed) add("pos_time_since_payment_confirmed_ms", paymentConfirmedTimestamp)
        return properties
    }

    @Synchronized
    fun reset() {
        checkoutGeneration++
        checkoutStartedTimestamp = null
        orderSyncCompletedTimestamp = null
        paymentConfirmedTimestamp = null
        syncStrategy = "unknown"
        interactionWithCustomerStartedTimestamp = null
        orderSyncSuccessTimestamp = null
        readerReadyForPaymentTimestamp = null
        cardTappedTimestamp = null
        checkoutButtonTapsCount = 0
    }
}
