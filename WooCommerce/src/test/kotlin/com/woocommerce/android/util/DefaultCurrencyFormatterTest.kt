package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Locale

@ExperimentalCoroutinesApi
class DefaultCurrencyFormatterTest : BaseUnitTest() {
    private lateinit var formatter: CurrencyFormatter
    private val localeProvider: LocaleProvider = mock()
    private val wcStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()

    @Test
    fun `when the selected site changes the default currency code updates`() =
        runTest {
            setupSitesFlow()
            // When the selected site currency code is empty
            var result = formatter.formatAmountWithCurrency(113.7)
            // Then use the locale default
            assertThat(result).doesNotContain("\$US").contains("$")

            // When the selected site change to a site with currencyCode ARS
            advanceTimeBy(1_100)
            result = formatter.formatAmountWithCurrency(113.7)
            // Then the formatted currency contains ARS
            assertThat(result).contains("ARS")

            // When the selected site change to a site with currencyCode USD
            advanceTimeBy(1_000)
            result = formatter.formatAmountWithCurrency(113.7)
            // Then the formatted currency contains $ (because current locale is US)
            assertThat(result).doesNotContain("\$US").contains("$")
        }

    @Test
    fun `when fetching site settings are null then retry fetching settings with exponential backoff`() =
        runTest {
            // First time fetch site setting will return an invalid response error
            // Second time fetch site setting will return an empty success response
            // Third time fetch site setting will return a success response
            setupExponentialBackoff()
            // When fetching site settings fails or is null
            // Then the app will retry fetching settings with exponential backoff
            advanceTimeBy(5_000)
            val result = formatter.formatAmountWithCurrency(113.7)
            // Then the formatted currency contains ARS
            assertThat(result).contains("ARS")
        }

    private fun setupSitesFlow() {
        val firstSite = SiteModel().also { it.id = 1 }
        val secondSite = SiteModel().also { it.id = 2 }
        val sitesFlow = flow<SiteModel> {
            emit(secondSite)
            delay(1_000)
            emit(firstSite)
            delay(1_000)
            emit(secondSite)
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
        whenever(wcStore.getSiteSettings(firstSite)).thenReturn(firstSettings)
        whenever(wcStore.getSiteSettings(secondSite)).thenReturn(secondSettings)
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)
        formatter = CurrencyFormatter(
            wcStore = wcStore,
            selectedSite = selectedSite,
            localeProvider = localeProvider,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    private suspend fun setupExponentialBackoff() {
        val site = SiteModel().also { it.id = 1 }
        val sitesFlow = flow { emit(site) }
        val siteSettings = WCSettingsModel(
            localSiteId = 1,
            currencyCode = "ARS",
            currencyPosition = WCSettingsModel.CurrencyPosition.RIGHT_SPACE,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2
        )
        whenever(selectedSite.observe()).thenReturn(sitesFlow)
        // First time return an error
        val firstCall = WooResult<WCSettingsModel>(
            error = WooError(
                WooErrorType.INVALID_RESPONSE,
                BaseRequest.GenericErrorType.INVALID_RESPONSE
            )
        )
        // Second time return an empty response
        val secondCall = WooResult<WCSettingsModel>()
        // Third time return valid settings
        val thirdCall = WooResult(siteSettings)
        whenever(wcStore.fetchSiteGeneralSettings(site)).doReturn(firstCall, secondCall, thirdCall)
        formatter = CurrencyFormatter(
            wcStore = wcStore,
            selectedSite = selectedSite,
            localeProvider = localeProvider,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }
}
