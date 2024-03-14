package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

object PaymentUtils {
    fun isSupportedCurrency(
        currency: String,
        cardReaderConfigFor: CardReaderConfigForSupportedCountry
    ): Boolean = currency.equals(
        cardReaderConfigFor.currency,
        ignoreCase = true
    )

    fun convertToSmallestCurrencyUnit(value: BigDecimal, currencyCode: String): Long {
        val currencyObj = Currency.getInstance(currencyCode)
        val smallestCurrencyUnit = BigDecimal.TEN.pow(currencyObj.defaultFractionDigits)
        return value.multiply(smallestCurrencyUnit).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
