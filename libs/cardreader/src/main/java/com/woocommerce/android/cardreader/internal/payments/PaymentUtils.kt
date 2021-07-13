package com.woocommerce.android.cardreader.internal.payments

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

private const val USD_CURRENCY = "usd"
internal const val USD_TO_CENTS_DECIMAL_PLACES = 2

class PaymentUtils @Inject constructor() {
    // TODO cardreader Add support for other currencies
    fun convertBigDecimalInDollarsToIntegerInCents(amount: BigDecimal): Int {
        return amount
            // round to USD_TO_CENTS_DECIMAL_PLACES decimal places
            .setScale(USD_TO_CENTS_DECIMAL_PLACES, RoundingMode.HALF_UP)
            // convert dollars to cents
            .movePointRight(USD_TO_CENTS_DECIMAL_PLACES)
            .intValueExact()
    }

    // TODO Add Support for other currencies
    fun isSupportedCurrency(currency: String): Boolean = currency.equals(USD_CURRENCY, ignoreCase = true)
}
