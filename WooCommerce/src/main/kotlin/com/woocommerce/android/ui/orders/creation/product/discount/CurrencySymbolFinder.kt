package com.woocommerce.android.ui.orders.creation.product.discount

import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.util.Locale
import javax.inject.Inject

class CurrencySymbolFinder @Inject constructor() {
    fun findCurrencySymbol(currencyCode: String): String {
        return WCCurrencyUtils.getLocalizedCurrencySymbolForCode(
            currencyCode,
            Locale.getDefault()
        )
    }
}
