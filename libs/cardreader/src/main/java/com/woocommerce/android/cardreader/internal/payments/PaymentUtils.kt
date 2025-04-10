package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.LOG_TAG
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

class PaymentUtils(private val logWrapper: LogWrapper) {
    fun isSupportedCurrency(
        currency: String,
        cardReaderConfigFor: CardReaderConfigForSupportedCountry
    ): Boolean = currency.equals(
        cardReaderConfigFor.currency,
        ignoreCase = true
    )

    fun convertToSmallestCurrencyUnit(value: BigDecimal, currencyCode: String): Long {
        return try {
            val currencyObj = Currency.getInstance(currencyCode)
            val smallestCurrencyUnit = BigDecimal.TEN.pow(currencyObj.defaultFractionDigits)
            value.multiply(smallestCurrencyUnit).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: IllegalArgumentException) {
            logWrapper.e(
                LOG_TAG,
                "Failed to convert to smallest currency unit for currency code: $currencyCode. " +
                    "value: $value. Exception: ${e.message}"
            )
            val fallbackScale = 2
            val fallbackSmallestUnit = BigDecimal.TEN.pow(fallbackScale)
            value.multiply(fallbackSmallestUnit).setScale(0, RoundingMode.HALF_UP).toLong()
        }
    }
}
