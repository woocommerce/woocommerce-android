package com.woocommerce.android.cardreader.internal.payments

import android.icu.util.Currency
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import java.math.BigDecimal
import java.math.RoundingMode

object PaymentUtils {
    fun isSupportedCurrency(
        currency: String,
        cardReaderConfigFor: CardReaderConfigForSupportedCountry
    ): Boolean = currency.equals(
        cardReaderConfigFor.currency, ignoreCase = true
    )

    fun convertToSmallestCurrencyUnit(value: BigDecimal, currency: Currency): Long {
        val smallestCurrencyUnit = BigDecimal.TEN.pow(currency.defaultFractionDigits)
        return value.multiply(smallestCurrencyUnit).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun fromCurrencyCode(currencyCode: String): Currency = Currency.getInstance(currencyCode)
}
