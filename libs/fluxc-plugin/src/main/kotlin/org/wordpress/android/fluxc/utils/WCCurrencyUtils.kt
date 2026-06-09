package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

object WCCurrencyUtils {
    /**
     * Formats the given [rawValue] as a decimal based on the site's currency settings as stored in [siteSettings].
     *
     * Currency symbol and placement are not handled.
     */
    fun formatCurrencyForDisplay(rawValue: Double, siteSettings: Settings, locale: Locale? = null): String {
        return formatCurrencyForDisplay(
            rawValue = rawValue,
            currencyDecimalNumber = siteSettings.currencyDecimalNumber,
            currencyDecimalSeparator = siteSettings.currencyDecimalSeparator,
            currencyThousandSeparator = siteSettings.currencyThousandSeparator,
            locale = locale
        )
    }

    fun formatCurrencyForDisplay(
        rawValue: Double,
        currencyDecimalNumber: Int,
        currencyDecimalSeparator: String,
        currencyThousandSeparator: String,
        locale: Locale? = null
    ): String {
        val pattern = if (currencyDecimalNumber > 0) {
            "#,##0.${"0".repeat(currencyDecimalNumber)}"
        } else {
            "#,##0"
        }

        val decimalFormat = locale?.let { DecimalFormat(pattern, DecimalFormatSymbols(locale)) }
            ?: DecimalFormat(pattern)

        decimalFormat.decimalFormatSymbols = decimalFormat.decimalFormatSymbols.apply {
            // If no decimal separator is set, keep whatever the system default is
            currencyDecimalSeparator.takeIf { it.isNotEmpty() }?.let {
                decimalSeparator = it[0]
            }
            // If no thousands separator is set, assume it's intentional and clear it in the formatter
            currencyThousandSeparator.takeIf { it.isNotEmpty() }?.let {
                groupingSeparator = it[0]
            } ?: run { decimalFormat.isGroupingUsed = false }
        }

        return decimalFormat.format(rawValue)
    }

    fun cleanFullFormattedCurrencyInput(text: CharSequence?, decimals: Int): BigDecimal? {
        val nonNumericPattern = Regex("[^0-9\\-]")
        var cleanValue = text.toString().replace(nonNumericPattern, "").toBigDecimalOrNull() ?: return null

        if (decimals > 0) {
            cleanValue = cleanValue.divide(BigDecimal(10f.pow(decimals).toInt()), decimals, HALF_UP)
        }

        return cleanValue
    }

    /**
     * Given a locale and an ISO 4217 currency code (e.g. USD), returns the currency symbol for that currency,
     * localized to the locale.
     *
     * Will return the [currencyCode] if it's found not to be a valid currency code.
     */
    fun getLocalizedCurrencySymbolForCode(currencyCode: String, locale: Locale): String {
        return try {
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (_: IllegalArgumentException) {
            AppLog.e(
                T.UTILS,
                "Error finding valid currency symbol for currency code [$currencyCode] in locale [$locale]"
            )
            currencyCode
        }
    }
}
