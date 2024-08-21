package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class SelectPaymentMethodCurrencyMissMatchLog @Inject constructor() {
    operator fun invoke(storeCurrency: String, orderCurrency: String) {
        if (!storeCurrency.equals(orderCurrency, ignoreCase = true)) {
            val message = "⚠️ Order currency: $orderCurrency differs from store's currency: " +
                "$storeCurrency which can lead to payment methods being unavailable"
            WooLog.w(WooLog.T.ORDERS, message)
        }
    }
}
