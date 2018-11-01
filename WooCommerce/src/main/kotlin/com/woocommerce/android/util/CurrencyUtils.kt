package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog.T
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Since the WooCommerce platform supports some unconventional ways of displaying currency,
 * normal currency formatting is not possible. This class serves as a temporary currency formatting service.
 * There is a ticket open to come up with a more permanent solution.
 *
 * @see [Correctly display currency](https://github.com/woocommerce/woocommerce-android/issues/148)
 */
@Suppress("MemberVisibilityCanBePrivate")
object CurrencyUtils {
    /**
     * Map of currency code to currency symbol. Ex: USD = $
     */
    private val symbolMap: MutableMap<String, String> = mutableMapOf()

    /**
     * Formats the value with two decimal places
     */
    val currencyFormatter: DecimalFormat by lazy {
        DecimalFormat("0.00")
    }

    /**
     * Formats the [rawValue] to two decimal places, and returns the value as a currency
     * string with the appropriate currency symbol.
     *
     * @param context The active context
     * @param rawValue The value to format as currency
     * @param currencyCode The ISO 4217 currency code (ex: USD)
     */
    fun currencyString(context: Context, rawValue: Double, currencyCode: String): String {
        val symbol = getCurrencySymbol(currencyCode)
        return if (rawValue < 0) {
            context.getString(
                    R.string.currency_total_negative, symbol, currencyFormatter.format(rawValue.absoluteValue))
        } else {
            context.getString(R.string.currency_total, symbol, currencyFormatter.format(rawValue))
        }
    }

    fun currencyString(context: Context, rawValue: String?, currencyCode: String) =
            currencyString(context, rawValue?.toDouble() ?: 0.0, currencyCode)

    /**
     * Rounds the [rawValue] to the nearest int, and returns the value as a currency
     * string with the appropriate currency symbol.
     *
     * @param context The active context
     * @param rawValue The value to format as currency
     * @param currencyCode The ISO 4217 currency code (ex: USD)
     */
    fun currencyStringRounded(context: Context, rawValue: Double, currencyCode: String): String {
        val symbol = getCurrencySymbol(currencyCode)
        val roundedValue = rawValue.roundToInt()

        // if the passed currency code is the same as the currency code of the current locale, format
        // the currency using the locale's currency formatter
        val locale = Locale.getDefault()
        try {
            if (Currency.getInstance(locale)?.currencyCode.equals(currencyCode)) {
                val formatter = NumberFormat.getCurrencyInstance(locale)
                return formatter.format(roundedValue).removeSuffix(".00")
            }
        } catch (e: IllegalArgumentException) {
            WooLog.e(T.UTILS, "Error finding valid currency instance for currency code [$currencyCode]", e)
        }

        // store must be using a currency that's different than that of the current locale so fall
        // back to our default currency format
        return if (rawValue < 0) {
            context.getString(R.string.currency_total_negative, symbol, roundedValue.absoluteValue.toString())
        } else {
            context.getString(R.string.currency_total, symbol, roundedValue.toString())
        }
    }

    /**
     * Returns the currency symbol for [currencyCode]. If no match is found, returns
     * an empty string. These values are stored in a map for fast reference.
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return symbolMap.getOrPut(currencyCode) {
            try {
                return Currency.getInstance(currencyCode).symbol
            } catch (e: IllegalArgumentException) {
                WooLog.e(T.UTILS, "Error finding valid currency symbol for currency code [$currencyCode]", e)
            }
            return ""
        }
    }
}
