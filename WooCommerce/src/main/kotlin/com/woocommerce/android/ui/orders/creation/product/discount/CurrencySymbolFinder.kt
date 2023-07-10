package com.woocommerce.android.ui.orders.creation.product.discount

import java.util.Currency
import javax.inject.Inject

class CurrencySymbolFinder @Inject constructor() {
    fun findCurrencySymbol(currencyCode: String): String {
        return Currency.getInstance(currencyCode).symbol
    }
}