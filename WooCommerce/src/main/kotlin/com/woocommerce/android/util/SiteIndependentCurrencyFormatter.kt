package com.woocommerce.android.util

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.util.locale.LocaleProvider
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class SiteIndependentCurrencyFormatter @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val crashLogging: CrashLogging,
) {
    /**
     * Returns formatted amount with currency symbol - eg. $113.5 for EN/USD or 113,5€ for FR/EUR.
     */
    fun formatAmountWithCurrency(amount: Double, currencyCode: String): String {
        val locale = localeProvider.provideLocale() ?: Locale.getDefault()
        val formatter = NumberFormat.getCurrencyInstance(locale)

        formatter.currency = try {
            if (currencyCode.isEmpty()) {
                Currency.getInstance(locale)
            } else {
                Currency.getInstance(currencyCode)
            }
        } catch (e: IllegalArgumentException) {
            crashLogging.sendReport(
                exception = e,
                message = "Invalid currency code: $currencyCode"
            )
            Currency.getInstance(locale)
        }
        return formatter.format(amount)
    }
}
