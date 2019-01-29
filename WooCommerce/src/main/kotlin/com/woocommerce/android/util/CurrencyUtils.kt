package com.woocommerce.android.util

import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object CurrencyUtils {
    private const val ONE_THOUSAND = 1000
    private const val ONE_MILLION = 1000000

    /**
     * Formats the value with two decimal places
     */
    private val currencyFormatter: DecimalFormat by lazy {
        DecimalFormat("0.00")
    }

    /**
     * Formats the value with one decimal place
     */
    private val currencyFormatterRounded: DecimalFormat by lazy {
        DecimalFormat("0.0")
    }

    /**
     * Rounds the [rawValue] to the nearest int, and returns the value as a currency string.
     *
     * If the value is a thousand or more, we return it rounded to the nearest tenth
     * and suffixed with "k" (2500 -> 2.5k).
     *
     * Similarly, we add "m" for values a million or higher.
     *
     * @param rawValue The value to format as currency
     */
    fun currencyStringRounded(rawValue: Double): String {
        val roundedValue = rawValue.roundToInt().toDouble()
        if (roundedValue.absoluteValue >= ONE_MILLION) {
            return currencyFormatterRounded.format(roundedValue / ONE_MILLION) + "m"
        } else if (roundedValue.absoluteValue >= ONE_THOUSAND) {
            return currencyFormatterRounded.format(roundedValue / ONE_THOUSAND) + "k"
        } else {
            return currencyFormatter.format(rawValue).toString().removeSuffix(".00")
        }
    }
}
