package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore

typealias FormatCurrencyRounded = (rawValue: Double, currencyCode: String) -> String

class CurrencyFormatter(private val wcStore: WooCommerceStore, private val selectedSite: SelectedSite) {
    fun formatCurrency(rawValue: String, currencyCode: String, applyDecimalFormatting: Boolean): String {
        return wcStore.formatCurrencyForDisplay(rawValue, selectedSite.get(), currencyCode, applyDecimalFormatting)
    }

    /**
     * Utility function that returns a reduced function for formatting currencies for orders.
     *
     * For order objects, we generally want to show exact values, and the currency used can be set once at a global
     * level - then the same function can be used for all the various currency fields of an order.
     *
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return a function which, given a raw amount as a String, returns the String formatted for display as a currency
     */
    fun buildFormatter(currencyCode: String) = { rawValue: String? ->
        formatCurrency(rawValue ?: "0.0", currencyCode, true)
    }
}
