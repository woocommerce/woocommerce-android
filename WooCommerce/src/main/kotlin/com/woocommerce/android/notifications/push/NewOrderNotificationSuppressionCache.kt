package com.woocommerce.android.notifications.push

import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory record of orders this device moved into a notifiable status, used to suppress the
 * new order push notification triggered by that same action.
 */
@Singleton
class NewOrderNotificationSuppressionCache @Inject constructor() {
    private val entries = Collections.synchronizedSet(mutableSetOf<Entry>())

    fun recordOrderCreated(siteId: Long, orderId: Long, statusKey: String) {
        if (statusKey in NOTIFIABLE_STATUSES) {
            entries += Entry(siteId, orderId)
        }
    }

    /**
     * Core notifies a status change only when the order moves from a non-notifiable status into
     * a notifiable one.
     */
    fun recordOrderStatusChanged(siteId: Long, orderId: Long, previousStatusKey: String?, newStatusKey: String) {
        if (newStatusKey in NOTIFIABLE_STATUSES && previousStatusKey !in NOTIFIABLE_STATUSES) {
            entries += Entry(siteId, orderId)
        }
    }

    fun consume(siteId: Long, orderId: Long): Boolean = entries.remove(Entry(siteId, orderId))

    private data class Entry(val siteId: Long, val orderId: Long)

    private companion object {
        // Mirrors NewOrderNotificationTrigger::NOTIFIABLE_STATUSES in WooCommerce core
        val NOTIFIABLE_STATUSES = setOf(
            CoreOrderStatus.PROCESSING.value,
            CoreOrderStatus.ON_HOLD.value,
            CoreOrderStatus.COMPLETED.value,
            "pre-order",
            "pre-ordered",
            "partial-payment",
        )
    }
}
