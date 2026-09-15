package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.subscription.SubscriptionRepository
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether an order was sold as an initial subscription purchase, which makes it
 * ineligible for in-person payment collection and Interac refunds (paying such an order in the app
 * doesn't set up the recurring subscription, and a card-present charge can't be tokenized for future
 * automatic renewals).
 *
 * It uses `/subscriptions?parent={orderId}`, which returns the subscription for parent/initial orders
 * but not for renewal orders — renewals are intentionally allowed, since the subscription already
 * exists and collecting/refunding simply records the payment against it.
 *
 * On subscriptions-enabled stores this fails closed: if the status can't be determined (network
 * error), the order is treated as ineligible.
 */
@Singleton
class OrderSubscriptionChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val subscriptionRepository: SubscriptionRepository,
) {
    private val cache = ConcurrentHashMap<Long, Boolean>()

    /**
     * Returns true only when the order is confirmed to be free of a parent subscription.
     */
    suspend fun isOrderFreeOfSubscriptions(order: Order): Boolean {
        if (order.getProductIds().isEmpty()) return true
        // Plugin inactive → the order can't be a subscription.
        if (wooCommerceStore.getActiveSitePlugin(selectedSite.get(), WOO_SUBSCRIPTIONS) == null) return true
        cache[order.id]?.let { return it }

        val result = subscriptionRepository.fetchSubscriptionsByOrderId(order.id, selectedSite.get())
        return if (result.isError) {
            // Fail closed and don't cache, so a transient failure can recover on the next attempt.
            false
        } else {
            (result.model?.isEmpty() ?: false).also { cache[order.id] = it }
        }
    }
}
