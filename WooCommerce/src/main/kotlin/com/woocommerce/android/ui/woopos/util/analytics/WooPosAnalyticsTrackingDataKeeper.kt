package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosAnalyticsTrackingDataKeeper @Inject constructor(private val elapsedTime: WooPosElapsedTime) {
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
    var readerConnectedAtCheckout: Boolean? = null
        private set
    var readerConnectedTimestamp: Long? = null
        private set

    // Latest disconnected to connected transition before this attempt's reader-ready. Preparation starts no earlier
    // than this. Frozen once the reader is ready, so a later reconnect never changes that attempt's preparation.
    var latestReaderConnectedTimestamp: Long? = null
        private set

    // Mirrors the live reader status, so reset() leaves it alone. Null until the first status, so unknown is not
    // reported as disconnected.
    private var isReaderConnected: Boolean? = null

    val millisecondsWaitingForReaderConnection: Long?
        get() {
            val interactionStarted = interactionWithCustomerStartedTimestamp ?: return null
            val readerConnected = readerConnectedTimestamp ?: return null
            return readerConnected - interactionStarted
        }

    // Starts at the latest of order sync, reader connection and the start of this attempt, so neither connection
    // waiting nor time on a previous attempt's error screen counts as preparation.
    val millisecondsFromOrderSyncToReaderReady: Long?
        get() {
            val orderSync = orderSyncSuccessTimestamp ?: return null
            val readerReady = readerReadyForAttemptTimestamp ?: return null
            val preparationStart = maxOf(
                orderSync,
                latestReaderConnectedTimestamp ?: orderSync,
                paymentAttemptStartedTimestamp ?: orderSync,
            )
            return (readerReady - preparationStart).takeIf { it >= 0 }
        }

    val readerReadyTimingProperties: Map<String, String>
        get() = buildMap {
            readerConnectedAtCheckout?.let { put(READER_CONNECTED_AT_CHECKOUT, it.toString()) }
            millisecondsWaitingForReaderConnection?.let {
                put(MILLISECONDS_WAITING_FOR_READER_CONNECTION, it.toString())
            }
            millisecondsFromOrderSyncToReaderReady?.let {
                put(MILLISECONDS_FROM_ORDER_SYNC_TO_READER_READY, it.toString())
            }
            readerReadySignal?.let { put(ReaderReadySignal.READER_READY_SIGNAL, it.value) }
        }

    fun startCustomerInteraction(timestamp: Long) {
        reset()
        interactionWithCustomerStartedTimestamp = timestamp
        readerConnectedAtCheckout = isReaderConnected
    }

    fun onReaderConnectionChanged(isConnected: Boolean, timestamp: Long) {
        val wasConnected = isReaderConnected
        isReaderConnected = isConnected
        val isNewConnection = isConnected && wasConnected == false
        if (!isNewConnection) return
        if (readerReadyForAttemptTimestamp == null) {
            latestReaderConnectedTimestamp = timestamp
        }
        if (readerConnectedAtCheckout == false && readerConnectedTimestamp == null) {
            readerConnectedTimestamp = timestamp
        }
    }

    fun startPaymentAttempt(timestamp: Long) {
        readerReadyForAttemptTimestamp = null
        readerReadySignal = null
        paymentAttemptStartedTimestamp = timestamp
    }

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
        readerReadyForAttemptTimestamp = null
        paymentAttemptStartedTimestamp = null
        readerReadySignal = null
        cardTappedTimestamp = null
        checkoutButtonTapsCount = 0
        readerConnectedAtCheckout = null
        readerConnectedTimestamp = null
        latestReaderConnectedTimestamp = null
    }

    private companion object {
        const val READER_CONNECTED_AT_CHECKOUT = "reader_connected_at_checkout"
        const val MILLISECONDS_WAITING_FOR_READER_CONNECTION = "milliseconds_waiting_for_reader_connection"
        const val MILLISECONDS_FROM_ORDER_SYNC_TO_READER_READY = "milliseconds_from_order_sync_to_reader_ready"
    }
}
