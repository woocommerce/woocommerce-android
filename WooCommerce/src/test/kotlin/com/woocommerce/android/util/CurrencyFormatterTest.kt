package com.woocommerce.android.util

import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

@ExperimentalCoroutinesApi
class CurrencyFormatterTest : BaseUnitTest() {
    private lateinit var formatter: CurrencyFormatter
    private val localeProvider: LocaleProvider = mock()

    @Before
    fun setup() {
        formatter = CurrencyFormatter(
            wcStore = mock(),
            selectedSite = mock(),
            localeProvider = localeProvider,
            appCoroutineScope = TestCoroutineScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `given fr locale, when formatting amount, then comma used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).contains("113,7")
    }

    @Test
    fun `given us locale, when formatting amount, then dot used as decimal point`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).contains("113.7")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then $US displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).contains("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then $ displayed`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).doesNotContain("\$US").contains("$")
    }

    @Test
    fun `given fr locale, when formatting dollar amount, then currency at the end`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.FRANCE)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).endsWith("\$US")
    }

    @Test
    fun `given us locale, when formatting dollar amount, then currency at the beginning`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        val result = formatter.formatAmountWithCurrency(113.7, "USD")

        assertThat(result).startsWith("$")
    }
}
