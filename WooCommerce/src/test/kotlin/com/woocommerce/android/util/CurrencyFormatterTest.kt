package com.woocommerce.android.util

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

class CurrencyFormatterTest : BaseUnitTest() {
    private lateinit var formatter: CurrencyFormatter
    private val localeProvider: LocaleProvider = mock()

    @Before
    fun setup() {
        formatter = CurrencyFormatter(mock(), mock(), localeProvider)
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `given fr locale, when formatting amount, then comma used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).contains("113,7")
    }

    @Test
    fun `given us locale, when formatting amount, then dot used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).contains("113.7")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then $US displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).contains("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then $ displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).doesNotContain("\$US").contains("$")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then currency at the end`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).endsWith("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then currency at the beginning`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency("USD", 113.7)

        assertThat(result).startsWith("$")
    }
}
