package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.*

@ExperimentalCoroutinesApi
class DefaultCurrencyFormatterTest : BaseUnitTest() {
    private lateinit var formatter: CurrencyFormatter
    private val localeProvider: LocaleProvider = mock()
    private val wcStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()

    @Before
    fun setup() {
        val firstSite = SiteModel().also { it.id = 1 }
        val secondSite = SiteModel().also { it.id = 2 }
        val thirdSite = SiteModel().also { it.id = 3 }
        val sitesFlow = flow<SiteModel> {
            emit(firstSite)
            delay(1_000)
            emit(secondSite)
            delay(1_000)
            emit(thirdSite)
        }
        val firstSettings = WCSettingsModel(
            localSiteId = 1,
            currencyCode = "ARS",
            currencyPosition = WCSettingsModel.CurrencyPosition.RIGHT_SPACE,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2
        )
        val secondSettings = firstSettings.copy(currencyCode = "USD")
        whenever(selectedSite.observe()).thenReturn(sitesFlow)
        whenever(wcStore.getSiteSettings(secondSite)).thenReturn(firstSettings)
        whenever(wcStore.getSiteSettings(thirdSite)).thenReturn(secondSettings)
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)
        formatter = CurrencyFormatter(
            wcStore = wcStore,
            selectedSite = selectedSite,
            localeProvider = localeProvider,
            appCoroutineScope = TestCoroutineScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when the selected site changes the default currency code updates`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // When the selected site currency code is empty
            var result = formatter.formatAmountWithCurrency(113.7)
            // Then use the locale default
            assertThat(result).doesNotContain("\$US").contains("$")

            // When the selected site change to a site with currencyCode ARS
            advanceTimeBy(1_000)
            result = formatter.formatAmountWithCurrency(113.7)
            // Then the formatted currency contains ARS
            assertThat(result).contains("ARS")

            // When the selected site change to a site with currencyCode USD
            advanceTimeBy(1_000)
            result = formatter.formatAmountWithCurrency(113.7)
            // Then the formatted currency contains $ (because current locale is US)
            assertThat(result).doesNotContain("\$US").contains("$")
        }
}
