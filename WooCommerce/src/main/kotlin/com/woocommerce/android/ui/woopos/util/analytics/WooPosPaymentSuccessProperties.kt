package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosPaymentSuccessProperties @Inject constructor(
    private val paymentUtils: PaymentUtils,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    operator fun invoke(order: Order): Map<String, Any> = mapOf(
        "amount_normalized" to paymentUtils.convertToSmallestCurrencyUnit(order.total, order.currency),
        "currency" to order.currency,
        "order_id" to order.id,
        "country" to (wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: "unknown"),
    )
}
