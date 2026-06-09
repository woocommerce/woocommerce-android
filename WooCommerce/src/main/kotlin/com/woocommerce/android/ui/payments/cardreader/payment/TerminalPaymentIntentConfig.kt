package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.cardreader.payments.PaymentInfo
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

object TerminalPaymentIntentConfig {
    fun calculateApplicationFeeInCents(
        countryCode: String,
        orderTotal: BigDecimal,
        currencyCode: String,
    ): Long? =
        when (countryCode.uppercase(Locale.ROOT)) {
            CANADA_COUNTRY_CODE -> CANADA_FEE_FLAT_IN_CENTS
            AUSTRALIA_COUNTRY_CODE -> calculatePercentageFeeInCents(
                orderTotal = orderTotal,
                currencyCode = currencyCode,
                percentage = AUSTRALIA_FEE_PERCENTAGE,
            ) + AUSTRALIA_FEE_FLAT_IN_CENTS
            else -> null
        }

    fun cardPresentCaptureMethod(countryCode: String): PaymentInfo.CardPresentCaptureMethod? =
        when (countryCode.uppercase(Locale.ROOT)) {
            CANADA_COUNTRY_CODE,
            AUSTRALIA_COUNTRY_CODE -> PaymentInfo.CardPresentCaptureMethod.MANUAL_PREFERRED
            else -> null
        }

    fun terminalPaymentPreparation(
        countryCode: String,
        isWooPaymentsPreferred: Boolean,
        isTerminalPaymentPreparationRouteAvailableInCanada: Boolean,
    ): PaymentInfo.TerminalPaymentPreparation =
        when {
            !isWooPaymentsPreferred -> PaymentInfo.TerminalPaymentPreparation.NONE
            countryCode.equals(CANADA_COUNTRY_CODE, ignoreCase = true) &&
                isTerminalPaymentPreparationRouteAvailableInCanada ->
                PaymentInfo.TerminalPaymentPreparation.CANADA_INTERAC
            countryCode.equals(AUSTRALIA_COUNTRY_CODE, ignoreCase = true) ->
                PaymentInfo.TerminalPaymentPreparation.AUSTRALIA_CARD_PRESENT
            else -> PaymentInfo.TerminalPaymentPreparation.NONE
        }

    fun hasTerminalPaymentPreparationRoute(routes: List<String>): Boolean =
        routes.any {
            it.startsWith(PREPARE_TERMINAL_PAYMENT_ROUTE_PREFIX) &&
                it.endsWith(PREPARE_TERMINAL_PAYMENT_ROUTE_SUFFIX)
        }

    private fun calculatePercentageFeeInCents(
        orderTotal: BigDecimal,
        currencyCode: String,
        percentage: BigDecimal,
    ): Long {
        val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits
        return orderTotal
            .multiply(percentage)
            .movePointRight(fractionDigits)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    private const val CANADA_COUNTRY_CODE = "CA"
    private const val AUSTRALIA_COUNTRY_CODE = "AU"
    private const val PREPARE_TERMINAL_PAYMENT_ROUTE_PREFIX = "/wc/v3/payments/orders/"
    private const val PREPARE_TERMINAL_PAYMENT_ROUTE_SUFFIX = "/prepare_terminal_payment"
    private const val CANADA_FEE_FLAT_IN_CENTS = 15L
    private const val AUSTRALIA_FEE_FLAT_IN_CENTS = 10L
    private val AUSTRALIA_FEE_PERCENTAGE = BigDecimal("0.017")
}
