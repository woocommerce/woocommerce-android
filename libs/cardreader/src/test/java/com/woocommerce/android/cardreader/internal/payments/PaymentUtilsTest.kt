package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private const val NONE_USD_CURRENCY = "CZK"
private const val USD_CURRENCY = "USD"

@ExperimentalCoroutinesApi
class PaymentUtilsTest : CardReaderBaseUnitTest() {
    private val paymentUtils = PaymentUtils

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
}
