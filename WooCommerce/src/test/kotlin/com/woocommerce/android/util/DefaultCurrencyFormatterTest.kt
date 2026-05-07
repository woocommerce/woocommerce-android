package com.woocommerce.android.util

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils.generateSettings
import java.util.Locale

@ExperimentalCoroutinesApi
class DefaultCurrencyFormatterTest : BaseUnitTest() {
    private lateinit var formatter: CurrencyFormatter
    private val localeProvider: LocaleProvider = mock()
    private val wcStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val crashLogging: CrashLogging = mock()
    private val siteIndependentCurrencyFormatter: SiteIndependentCurrencyFormatter =
        SiteIndependentCurrencyFormatter(localeProvider, crashLogging)

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
    fun `when site settings are updated through observeAllSiteSettings the currency updates`() =
        runTest {
            // GIVEN
            val site = SiteModel().also { it.id = 1 }
            val siteFlow = flowOf(site)
            val settingsFlow = MutableStateFlow(mapOf<LocalId, Settings>())

            whenever(selectedSite.observe()).thenReturn(siteFlow)
            whenever(wcStore.observeAllSiteSettings()).thenReturn(settingsFlow)
            whenever(wcStore.getSiteSettings(site)).thenReturn(null)
            whenever(wcStore.fetchSiteGeneralSettings(site)).thenReturn(WooResult())
            whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

            formatter = CurrencyFormatter(
                wcStore = wcStore,
                selectedSite = selectedSite,
                siteIndependentCurrencyFormatter = siteIndependentCurrencyFormatter,
                localeProvider = localeProvider,
                appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
                dispatchers = coroutinesTestRule.testDispatchers
            )

            advanceTimeBy(5_000)
            var result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("$")

            val eurSettings = generateSettings(LocalId(1)).copy(currencyCode = "EUR")
            settingsFlow.value = mapOf(LocalId(1) to eurSettings)
            advanceTimeBy(100)
            result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("€")

            val gbpSettings = generateSettings(LocalId(1)).copy(currencyCode = "GBP")
            settingsFlow.value = mapOf(LocalId(1) to gbpSettings)
            advanceTimeBy(100)
            result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("£")
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

    @Test
    fun `when multiple sites have settings in observeAllSiteSettings map`() =
        runTest {
            // GIVEN
            val site1 = SiteModel().also { it.id = 1 }
            val site2 = SiteModel().also { it.id = 2 }
            val site3 = SiteModel().also { it.id = 3 }

            val settings1 = generateSettings(LocalId(1)).copy(currencyCode = "AUD")
            val settings2 = generateSettings(LocalId(2)).copy(currencyCode = "CAD")
            val settings3 = generateSettings(LocalId(3)).copy(currencyCode = "NZD")

            val settingsFlow = MutableStateFlow(
                mapOf(
                    LocalId(1) to settings1,
                    LocalId(2) to settings2,
                    LocalId(3) to settings3
                )
            )

            val siteFlow = flow {
                emit(site1)
                delay(1_000)
                emit(site2)
                delay(1_000)
                emit(site3)
            }

            whenever(selectedSite.observe()).thenReturn(siteFlow)
            whenever(wcStore.observeAllSiteSettings()).thenReturn(settingsFlow)
            whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

            formatter = CurrencyFormatter(
                wcStore = wcStore,
                selectedSite = selectedSite,
                siteIndependentCurrencyFormatter = siteIndependentCurrencyFormatter,
                localeProvider = localeProvider,
                appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
                dispatchers = coroutinesTestRule.testDispatchers
            )

            advanceTimeBy(100)
            var result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("A$")

            advanceTimeBy(1_000)
            result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("CA$")

            advanceTimeBy(1_000)
            result = formatter.formatAmountWithCurrency(100.0)
            assertThat(result).contains("NZ$")
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
        val firstSettings = generateSettings(LocalId(1)).copy(currencyCode = "ARS")
        val secondSettings = generateSettings(LocalId(2)).copy(currencyCode = "USD")

        val settingsMap = MutableStateFlow(mapOf<LocalId, Settings>())
        settingsMap.value = mapOf(
            LocalId(1) to firstSettings,
            LocalId(2) to secondSettings
        )

        whenever(selectedSite.observe()).thenReturn(sitesFlow)
        whenever(wcStore.observeAllSiteSettings()).thenReturn(settingsMap)
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)
        formatter = CurrencyFormatter(
            wcStore = wcStore,
            selectedSite = selectedSite,
            siteIndependentCurrencyFormatter = siteIndependentCurrencyFormatter,
            localeProvider = localeProvider,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    private suspend fun setupExponentialBackoff() {
        val site = SiteModel().also { it.id = 1 }
        val sitesFlow = flow { emit(site) }
        val siteSettings = generateSettings(LocalId(1)).copy(currencyCode = "ARS")

        val settingsMap = MutableStateFlow(mapOf<LocalId, Settings>())

        whenever(selectedSite.observe()).thenReturn(sitesFlow)
        whenever(wcStore.observeAllSiteSettings()).thenReturn(settingsMap)
        // First time return an error
        val firstCall = WooResult<Settings>(
            error = WooError(
                WooErrorType.INVALID_RESPONSE,
                BaseRequest.GenericErrorType.INVALID_RESPONSE
            )
        )
        // Second time return an empty response
        val secondCall = WooResult<Settings>()
        // Third time return valid settings
        val thirdCall = WooResult(siteSettings)
        whenever(wcStore.fetchSiteGeneralSettings(site)).doReturn(firstCall, secondCall, thirdCall)
        formatter = CurrencyFormatter(
            wcStore = wcStore,
            selectedSite = selectedSite,
            siteIndependentCurrencyFormatter = siteIndependentCurrencyFormatter,
            localeProvider = localeProvider,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }
}
