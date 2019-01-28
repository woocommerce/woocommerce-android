package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore

class CurrencyFormatter(private val wcStore: WooCommerceStore, private val selectedSite: SelectedSite) {
    fun formatCurrency(rawValue: String, currencyCode: String, applyDecimalFormatting: Boolean): String {
        return wcStore.formatCurrencyForDisplay(rawValue, selectedSite.get(), currencyCode, applyDecimalFormatting)
    }
}
