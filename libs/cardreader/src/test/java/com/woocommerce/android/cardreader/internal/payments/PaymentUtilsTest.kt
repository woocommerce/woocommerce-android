package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

private const val NONE_USD_CURRENCY = "CZK"
private const val USD_CURRENCY = "USD"

class PaymentUtilsTest : CardReaderBaseUnitTest() {
    private val paymentUtils = PaymentUtils()

    @Test
    fun `given supported country, when not supported currency invoked, then false returned`() = testBlocking {
        val result = paymentUtils.isSupportedCurrency(NONE_USD_CURRENCY, CardReaderConfigForCanada)

        assertThat(result).isFalse()
    }

    @Test
    fun `given supported country, when is currency supported invoked, then true returned`() = testBlocking {
        val result = paymentUtils.isSupportedCurrency(USD_CURRENCY, CardReaderConfigForUSA)

        assertThat(result).isTrue()
    }

    @Test
    fun `when amount $1, then it gets converted to 100 cents`() = testBlocking {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1))

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `when amount $1 005cents, then it gets rounded down to 100 cents`() = testBlocking {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.005))

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `when amount $1 006cents, then it gets rounded up to 101 cents`() = testBlocking {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.006))

        assertThat(result).isEqualTo(101)
    }

    @Test
    fun `when amount $1 99 cents, then it gets converted to 199 cents`() = testBlocking {
        val result = paymentUtils.convertBigDecimalInDollarsToLongInCents(BigDecimal(1.99))

        assertThat(result).isEqualTo(199)
    }
}
