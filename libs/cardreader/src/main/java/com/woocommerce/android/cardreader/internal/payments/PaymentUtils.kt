package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import java.math.BigDecimal
import java.math.RoundingMode

private const val CURRENCY_SCALE = 2

class PaymentUtils {
    fun isSupportedCurrency(
        currency: String,
        cardReaderConfigFor: CardReaderConfigForSupportedCountry
    ): Boolean = currency.equals(
        cardReaderConfigFor.currency, ignoreCase = true
    )
}

// TODO cardreader Add support for other currencies
fun BigDecimal.convertInCurrencyScaleOf2ToLongInCents(): Long {
    return this
        // round to USD_TO_CENTS_DECIMAL_PLACES decimal places
        .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
        // convert dollars to cents
        .movePointRight(CURRENCY_SCALE)
        .longValueExact()
}
