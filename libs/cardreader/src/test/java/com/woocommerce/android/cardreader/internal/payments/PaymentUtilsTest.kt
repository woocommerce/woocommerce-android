package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

private const val NONE_USD_CURRENCY = "CZK"
private const val USD_CURRENCY = "USD"

@Suppress("DoNotMockDataClass", "DoNotMockSealedClass")
@ExperimentalCoroutinesApi
class PaymentUtilsTest : CardReaderBaseUnitTest() {
    private lateinit var paymentUtils: PaymentUtils
    private lateinit var mockLogWrapper: LogWrapper

    @Before
    fun setup() {
        mockLogWrapper = mock()
        paymentUtils = PaymentUtils(mockLogWrapper)
    }

    @Test
    fun `given two decimal currency, given supported country, when not supported currency invoked, then false returned`() =
        testBlocking {
            val result = paymentUtils.isSupportedCurrency(NONE_USD_CURRENCY, CardReaderConfigForCanada)

            assertThat(result).isFalse()
        }

    @Test
    fun `given two decimal currency, given supported country, when is currency supported invoked, then true returned`() =
        testBlocking {
            val result = paymentUtils.isSupportedCurrency(USD_CURRENCY, CardReaderConfigForUSA)

            assertThat(result).isTrue()
        }

    @Test
    fun `given case difference, when is currency supported invoked, then true returned`() =
        testBlocking {
            val mockConfig = mock<CardReaderConfigForSupportedCountry>()
            whenever(mockConfig.currency).thenReturn("USD")

            val result = paymentUtils.isSupportedCurrency("usd", mockConfig)

            assertThat(result).isTrue()
        }

    @Test
    fun `given empty currency, when is currency supported invoked, then false returned`() =
        testBlocking {
            val mockConfig = mock<CardReaderConfigForSupportedCountry>()
            whenever(mockConfig.currency).thenReturn("USD")

            val result = paymentUtils.isSupportedCurrency("", mockConfig)

            assertThat(result).isFalse()
        }

    @Test
    fun `given two decimal currency, when amount $1, then it gets converted to 100 cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(1),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `given two decimal currency, when amount $1 004cents, then it gets rounded down to 100 cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.004"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `given two decimal currency, when amount $1 005cents, then it gets rounded down to 101 cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.005"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `given two decimal currency, when amount $1 06 cents, then gets rounded up to 101 cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.006"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `given two decimal currency, when amount $1 99 cents, then it gets converted to 199 cents`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.99"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(199)
    }

    @Test
    fun `given zero decimal currency, when amount 300, then it gets converted to 300`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(300),
            ZERO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(300)
    }

    @Test
    fun `given zero decimal currency, when amount 100 and 5, then it gets converted to 101`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("100.5"),
            ZERO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `given zero decimal currency, when amount 100 and 4, then it gets converted to 100`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("100.4"),
            ZERO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `given three decimal currency, when amount 1000, then it gets converted to 1000`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.000"),
            THREE_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(1000)
    }

    @Test
    fun `given three decimal currency, when amount 1001, then it gets converted to 1001`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("1.001"),
            THREE_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(1001)
    }

    @Test
    fun `given three decimal currency, when amount 100, then it gets converted to 100000`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal(100),
            THREE_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(100000)
    }

    @Test
    fun `given negative amount, when converting to smallest unit, then correct negative value returned`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("-10.50"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(-1050)
    }

    @Test
    fun `given zero amount, when converting to smallest unit, then zero returned`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal.ZERO,
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `given very small amount, when converting to smallest unit, then correct value returned`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("0.01"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `given large amount, when converting to smallest unit, then correct value returned`() {
        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("9999.99"),
            TWO_DECIMAL_CURRENCY_CODE,
        )

        assertThat(result).isEqualTo(999999)
    }

    @Test
    fun `given invalid currency code, when converting to smallest unit, then fallback mechanism used`() {
        val invalidCurrencyCode = "INVALID"

        val result = paymentUtils.convertToSmallestCurrencyUnit(
            BigDecimal("10.50"),
            invalidCurrencyCode
        )

        assertThat(result).isEqualTo(1050)
    }

    private companion object {
        private const val TWO_DECIMAL_CURRENCY_CODE = "USD"
        private const val ZERO_DECIMAL_CURRENCY_CODE = "JPY"
        private const val THREE_DECIMAL_CURRENCY_CODE = "BHD"
    }
}
