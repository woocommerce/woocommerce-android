package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.cardreader.payments.PaymentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class TerminalPaymentIntentConfigTest {
    @Test
    fun `given Canada, when calculating application fee, then flat 15 cents returned`() {
        val fee = TerminalPaymentIntentConfig.calculateApplicationFeeInCents(
            countryCode = "CA",
            orderTotal = BigDecimal("18.00"),
            currencyCode = "CAD",
        )

        assertThat(fee).isEqualTo(15L)
    }

    @Test
    fun `given Australia, when calculating application fee, then percentage plus flat fee returned`() {
        val fee = TerminalPaymentIntentConfig.calculateApplicationFeeInCents(
            countryCode = "AU",
            orderTotal = BigDecimal("18.00"),
            currencyCode = "AUD",
        )

        assertThat(fee).isEqualTo(41L)
    }

    @Test
    fun `given unsupported country, when calculating application fee, then null returned`() {
        val fee = TerminalPaymentIntentConfig.calculateApplicationFeeInCents(
            countryCode = "US",
            orderTotal = BigDecimal("18.00"),
            currencyCode = "USD",
        )

        assertThat(fee).isNull()
    }

    @Test
    fun `given Canada or Australia, when resolving capture method, then manual preferred returned`() {
        assertThat(TerminalPaymentIntentConfig.cardPresentCaptureMethod("CA"))
            .isEqualTo(PaymentInfo.CardPresentCaptureMethod.MANUAL_PREFERRED)
        assertThat(TerminalPaymentIntentConfig.cardPresentCaptureMethod("AU"))
            .isEqualTo(PaymentInfo.CardPresentCaptureMethod.MANUAL_PREFERRED)
    }

    @Test
    fun `given unsupported country, when resolving capture method, then null returned`() {
        assertThat(TerminalPaymentIntentConfig.cardPresentCaptureMethod("US")).isNull()
    }

    @Test
    fun `given Canada with WCPay and route available or Australia with WCPay, when resolving terminal preparation, then country-specific value returned`() {
        assertThat(
            TerminalPaymentIntentConfig.terminalPaymentPreparation(
                countryCode = "CA",
                isWooPaymentsPreferred = true,
                isTerminalPaymentPreparationRouteAvailableInCanada = true,
            )
        )
            .isEqualTo(PaymentInfo.TerminalPaymentPreparation.CANADA_INTERAC)
        assertThat(
            TerminalPaymentIntentConfig.terminalPaymentPreparation(
                countryCode = "AU",
                isWooPaymentsPreferred = true,
                isTerminalPaymentPreparationRouteAvailableInCanada = false,
            )
        )
            .isEqualTo(PaymentInfo.TerminalPaymentPreparation.AUSTRALIA_CARD_PRESENT)
    }

    @Test
    fun `given unsupported country, when resolving terminal preparation, then none returned`() {
        assertThat(
            TerminalPaymentIntentConfig.terminalPaymentPreparation(
                countryCode = "US",
                isWooPaymentsPreferred = true,
                isTerminalPaymentPreparationRouteAvailableInCanada = false,
            )
        )
            .isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
    }

    @Test
    fun `given non-WooPayments gateway, when resolving terminal preparation, then none returned`() {
        assertThat(
            TerminalPaymentIntentConfig.terminalPaymentPreparation(
                countryCode = "AU",
                isWooPaymentsPreferred = false,
                isTerminalPaymentPreparationRouteAvailableInCanada = true,
            )
        )
            .isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
    }

    @Test
    fun `given Canada without route available, when resolving terminal preparation, then none returned`() {
        assertThat(
            TerminalPaymentIntentConfig.terminalPaymentPreparation(
                countryCode = "CA",
                isWooPaymentsPreferred = true,
                isTerminalPaymentPreparationRouteAvailableInCanada = false,
            )
        )
            .isEqualTo(PaymentInfo.TerminalPaymentPreparation.NONE)
    }

    @Test
    fun `given route list contains terminal preparation route, when checking route support, then true returned`() {
        val routes = listOf(
            "/wc/v3/orders",
            "/wc/v3/payments/orders/(?P<order_id>\\w+)/prepare_terminal_payment",
        )

        assertThat(TerminalPaymentIntentConfig.hasTerminalPaymentPreparationRoute(routes)).isTrue()
    }
}
