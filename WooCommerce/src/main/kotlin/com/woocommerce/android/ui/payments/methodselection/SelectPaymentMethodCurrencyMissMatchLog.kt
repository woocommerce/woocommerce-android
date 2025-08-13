package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class SelectPaymentMethodCurrencyMissMatchLog @Inject constructor(
    private val wooLog: WooLog,
) {
    operator fun invoke(storeCurrency: String, orderCurrency: String) {
        if (!storeCurrency.equals(orderCurrency, ignoreCase = true)) {
            val message = "⚠️ Order currency: $orderCurrency differs from store's currency: " +
                "$storeCurrency which can lead to payment methods being unavailable"
            wooLog.w(WooLog.T.ORDERS, message)
        }
    }
}
