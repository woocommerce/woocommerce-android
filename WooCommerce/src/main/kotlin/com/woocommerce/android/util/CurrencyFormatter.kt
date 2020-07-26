package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

typealias FormatCurrencyRounded = (rawValue: Double, currencyCode: String) -> String

class CurrencyFormatter(private val wcStore: WooCommerceStore, private val selectedSite: SelectedSite) {
    companion object {
        private const val ONE_THOUSAND = 1000
        private const val ONE_MILLION = 1000000

        // Formats the value to two decimal places
        private val currencyFormatter: DecimalFormat by lazy {
            DecimalFormat("0.00")
        }

        // Formats the value to one decimal place
        private val currencyFormatterRounded: DecimalFormat by lazy {
            DecimalFormat("0.0")
        }

        private fun currencyStringRounded(rawValue: Double): String {
            val roundedValue = rawValue.roundToInt().toDouble()
            return if (roundedValue.absoluteValue >= ONE_MILLION) {
                currencyFormatterRounded.format(roundedValue / ONE_MILLION) + "m"
            } else if (roundedValue.absoluteValue >= ONE_THOUSAND) {
                currencyFormatterRounded.format(roundedValue / ONE_THOUSAND) + "k"
            } else {
                currencyFormatter.format(roundedValue).toString().removeSuffix(".00")
            }
        }
    }

    /**
     * Formats a raw amount for display based on the WooCommerce site settings.
     *
     * @param rawValue the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrency(rawValue: String, currencyCode: String, applyDecimalFormatting: Boolean = true) =
            wcStore.formatCurrencyForDisplay(rawValue, selectedSite.get(), currencyCode, applyDecimalFormatting)

    /**
     * Formats the amount for display based on the WooCommerce site settings.
     *
     * @param amount the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrency(amount: BigDecimal, currencyCode: String, applyDecimalFormatting: Boolean = true) =
            formatCurrency(amount.toString(), currencyCode, applyDecimalFormatting)

    /**
     * Formats a raw amount for display based on the WooCommerce site settings, rounding the values to the nearest int.
     *
     * Additionally, if the value is a thousand or more, we return it rounded to the nearest tenth
     * and suffixed with "k" (2500 -> 2.5k).
     *
     * Similarly, we add "m" for values a million or higher.
     *
     * @param rawValue the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrencyRounded(rawValue: Double, currencyCode: String): String {
        val displayFormatted = currencyStringRounded(rawValue)
        return displayFormatted.takeIf { it.isNotEmpty() }?.let {
            return wcStore.formatCurrencyForDisplay(it, selectedSite.get(), currencyCode, false)
        }.orEmpty()
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

    /**
     * Utility function that returns a reduced function for formatting currencies for orders.
     *
     * For order objects, we generally want to show exact values, and the currency used can be set once at a global
     * level - then the same function can be used for all the various currency fields of an order.
     *
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return a function which, given an amount as a BigDecimal, returns the String formatted for display as a currency
     */
    fun buildBigDecimalFormatter(currencyCode: String) = { amount: BigDecimal ->
        formatCurrency(amount, currencyCode, true)
    }
}
