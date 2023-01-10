package com.woocommerce.android.util

import com.woocommerce.android.util.locale.LocaleProvider
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class SiteIndependentCurrencyFormatter @Inject constructor(
    private val localeProvider: LocaleProvider,
) {
    /**
     * Returns formatted amount with currency symbol - eg. $113.5 for EN/USD or 113,5€ for FR/EUR.
     */
    fun formatAmountWithCurrency(amount: Double, currencyCode: String): String {
        val locale = localeProvider.provideLocale() ?: Locale.getDefault()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = if (currencyCode.isEmpty()) {
            Currency.getInstance(locale)
        } else {
            Currency.getInstance(currencyCode)
        }
        return formatter.format(amount)
    }
}
