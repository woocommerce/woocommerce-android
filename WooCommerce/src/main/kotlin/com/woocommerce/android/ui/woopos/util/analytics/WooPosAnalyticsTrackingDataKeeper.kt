package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosAnalyticsTrackingDataKeeper @Inject constructor() {
    var interactionWithCustomerStartedTimestamp: Long? = null
    var orderSyncSuccessTimestamp: Long? = null
    var readerReadyForPaymentTimestamp: Long? = null
    var readerReadySignal: ReaderReadySignal? = null
    var cardTappedTimestamp: Long? = null
    var checkoutButtonTapsCount: Int = 0
    var readerConnectedAtCheckout: Boolean? = null
        private set
    var readerConnectedTimestamp: Long? = null
        private set

    // Mirrors the live reader status, so reset() leaves it alone.
    private var isReaderConnected = false

    val millisecondsWaitingForReaderConnection: Long?
        get() {
            val interactionStarted = interactionWithCustomerStartedTimestamp ?: return null
            val readerConnected = readerConnectedTimestamp ?: return null
            return readerConnected - interactionStarted
        }

    // Starts at the later of order sync and reader connection, so connection waiting is not counted as preparation.
    val millisecondsFromOrderSyncToReaderReady: Long?
        get() {
            val orderSync = orderSyncSuccessTimestamp ?: return null
            val readerReady = readerReadyForPaymentTimestamp ?: return null
            return readerReady - maxOf(orderSync, readerConnectedTimestamp ?: orderSync)
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
        val isNewConnection = isConnected && !isReaderConnected
        isReaderConnected = isConnected
        if (isNewConnection && readerConnectedAtCheckout == false && readerConnectedTimestamp == null) {
            readerConnectedTimestamp = timestamp
        }
    }

    fun reset() {
        interactionWithCustomerStartedTimestamp = null
        orderSyncSuccessTimestamp = null
        readerReadyForPaymentTimestamp = null
        readerReadySignal = null
        cardTappedTimestamp = null
        checkoutButtonTapsCount = 0
        readerConnectedAtCheckout = null
        readerConnectedTimestamp = null
    }

    private companion object {
        const val READER_CONNECTED_AT_CHECKOUT = "reader_connected_at_checkout"
        const val MILLISECONDS_WAITING_FOR_READER_CONNECTION = "milliseconds_waiting_for_reader_connection"
        const val MILLISECONDS_FROM_ORDER_SYNC_TO_READER_READY = "milliseconds_from_order_sync_to_reader_ready"
    }
}
