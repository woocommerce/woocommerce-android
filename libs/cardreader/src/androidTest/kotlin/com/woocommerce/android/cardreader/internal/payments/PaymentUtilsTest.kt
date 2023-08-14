package com.woocommerce.android.cardreader.internal.payments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class PaymentUtilsTest {
    private val paymentUtils = PaymentUtils

    @Test
    fun `whenAmount$1ThenItGetsConvertedTo100Cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1),
            paymentUtils.fromCurrencyCode(TWO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `whenAmount$1005CentsThenItGetsRoundedDownTo100Cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1.005),
            paymentUtils.fromCurrencyCode(TWO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `whenAmount$1006CentsThentGetsRoundedUpTo101Cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1.006),
            paymentUtils.fromCurrencyCode(TWO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `whenAmount$199CentsThenItGetsConvertedTo199Cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1.99),
            paymentUtils.fromCurrencyCode(TWO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(199)
    }

    @Test
    fun `whenAmount300ThenAndZeroDecimalCurrencyItGetsConvertedTo300`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(300),
            paymentUtils.fromCurrencyCode(ZERO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(300)
    }

    @Test
    fun `whenAmount100and5AndZeroDecimalCurrencyThenItGetsConvertedTo100`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(100.5),
            paymentUtils.fromCurrencyCode(ZERO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `whenAmount100and4AndZeroDecimalCurrencyThenItGetsConvertedTo100`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(100.4),
            paymentUtils.fromCurrencyCode(ZERO_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `whenAmount1000AndThreeDecimalCurrencyThenItGetsConvertedTo101`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1.000),
            paymentUtils.fromCurrencyCode(THREE_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(1000)
    }

    @Test
    fun `whenAmount1001AndThreeDecimalCurrencyThenItGetsConvertedTo101`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1.001),
            paymentUtils.fromCurrencyCode(THREE_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(1001)
    }

    @Test
    fun `whenAmount100DecimalCurrencyThenItGetsConvertedTo101`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(100),
            paymentUtils.fromCurrencyCode(THREE_DECIMAL_CURRENCY_CODE)
        )

        assertThat(result).isEqualTo(100000)
    }

    private companion object {
        private const val TWO_DECIMAL_CURRENCY_CODE = "USD"
        private const val ZERO_DECIMAL_CURRENCY_CODE = "JPY"
        private const val THREE_DECIMAL_CURRENCY_CODE = "BHD"
    }
}
