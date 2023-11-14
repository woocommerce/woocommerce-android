package com.woocommerce.android.util

import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

@ExperimentalCoroutinesApi
class SiteIndependentCurrencyFormatterTest : BaseUnitTest() {
    private val localeProvider: LocaleProvider = mock()

    private val currencyFormatter: SiteIndependentCurrencyFormatter = SiteIndependentCurrencyFormatter(
        localeProvider
    )

    @Test
    fun `given fr locale, when formatting amount, then comma used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).contains("113,7")
    }

    @Test
    fun `given us locale, when formatting amount, then dot used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).contains("113.7")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then $US displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).contains("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then $ displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).doesNotContain("\$US").contains("$")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then currency at the end`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).endsWith("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then currency at the beginning`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = currencyFormatter.formatAmountWithCurrency(113.7, "USD")

        Assertions.assertThat(result).startsWith("$")
    }
}
