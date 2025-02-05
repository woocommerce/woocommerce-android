package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

object WCCurrencyUtils {
    /**
     * Formats the given [rawValue] as a decimal based on the site's currency settings as stored in [siteSettings].
     *
     * Currency symbol and placement are not handled.
     */
    fun formatCurrencyForDisplay(rawValue: Double, siteSettings: WCSettingsModel, locale: Locale? = null): String {
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

    /**
     * Given a locale and an ISO 4217 currency code (e.g. USD), returns the currency symbol for that currency,
     * localized to the locale.
     *
     * Will return the [currencyCode] if it's found not to be a valid currency code.
     */
    fun getLocalizedCurrencySymbolForCode(currencyCode: String, locale: Locale): String {
        return try {
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (e: IllegalArgumentException) {
            AppLog.e(T.UTILS,
                    "Error finding valid currency symbol for currency code [$currencyCode] in locale [$locale]", e)
            currencyCode
        }
    }
}
