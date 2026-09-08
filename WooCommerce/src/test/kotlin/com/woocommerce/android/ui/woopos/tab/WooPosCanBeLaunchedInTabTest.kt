package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Launchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NonLaunchabilityReason
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NotLaunchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchabilityRefreshPolicy.ForceRefresh
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchabilityRefreshPolicy.UseCache
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.EnabledFeatures
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCanBeLaunchedInTabTest {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getWooCorePluginStatus: WooPosGetWooCorePluginStatus = mock()
    private val isFeatureSwitchEnabled: WooPosIsFeatureSwitchEnabled = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var sut: WooPosCanBeLaunchedInTab
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        runBlocking {
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("US", "USD"))
            whenever(getWooCorePluginStatus(any()))
                .thenReturn(WooPosWooCorePluginStatus.Active("10.0.0"))
            whenever(isFeatureSwitchEnabled(any(), anyOrNull())).thenReturn(Result.success(true))
        }
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            getWooCorePluginStatus = getWooCorePluginStatus,
            isFeatureSwitchEnabled = isFeatureSwitchEnabled,
            featureFlagRepository = featureFlagRepository,
            wooPosLog = mock()
        )
    }

    private fun settings(countryCode: String, currencyCode: String) = Settings(
        currencyCode = currencyCode,
        currencyPosition = CurrencyPosition.LEFT,
        currencyThousandSeparator = ",",
        currencyDecimalSeparator = ".",
        currencyDecimalNumber = 2,
        countryCode = countryCode,
        stateCode = "",
        address = "",
        address2 = "",
        city = "",
        postalCode = "",
        couponsEnabled = true,
    )

    // --- Happy paths ---

    @Test
    fun `given valid conditions, when invoked, then return Launchable and set cache`() = runTest {
        val result = sut(UseCache)
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    // --- No site ---

    @Test
    fun `given no site selected, when invoked, then NotLaunchable NoSiteSelected and no prefs touched`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val result = sut(UseCache)
        assertEquals(NotLaunchable(NonLaunchabilityReason.NoSiteSelected), result)
        verify(appPrefs, times(0)).setPOSLaunchableForSite(any())
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    // --- Currency checks ---

    @Test
    fun `given store currency does not match store country, when invoked, then NotLaunchable UnsupportedCurrency`() =
        runTest {
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("CA", "EUR"))

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
        }

    @Test
    fun `given a currency mismatch read from local settings, when invoked, then the positive cache survives`() =
        runTest {
            // Local settings can be stale: the merchant may have already fixed the currency
            // remotely. iOS decides this on settings it has just refreshed.
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("CA", "EUR"))

            sut(UseCache)

            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given a currency mismatch read from a fresh fetch, when invoked, then the positive cache is cleared`() =
        runTest {
            whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel))
                .thenReturn(WooResult(settings("CA", "EUR")))

            val result = sut(ForceRefresh)

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given no settings stored and a fetched currency mismatch, when invoked, then the cache is cleared`() =
        runTest {
            // The cached path falls back to a fetch when nothing is stored, and what that fetch
            // returns is as fresh as ForceRefresh would be.
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)
            whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel))
                .thenReturn(WooResult(settings("CA", "EUR")))

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), sut(UseCache))
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given a blank country code with no cached positive, when invoked, then UnknownNoPositiveCache`() = runTest {
        // The settings response can omit the country. That is an unknown, not an unsupported one.
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("", "USD"))

        val result = sut(UseCache)

        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, never()).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given a blank country code with cached positive, when invoked, then Launchable`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("", "USD"))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut(UseCache))
    }

    @Test
    fun `given every supported country with its currency, when invoked, then Launchable`() = runTest {
        val pairs = listOf(
            "US" to "USD", "PR" to "USD", "GB" to "GBP", "CA" to "CAD",
            "FI" to "EUR", "IE" to "EUR", "LU" to "EUR", "NL" to "EUR",
            "SG" to "SGD", "NZ" to "NZD", "AU" to "AUD",
        )

        pairs.forEach { (country, currency) ->
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings(country, currency))

            assertEquals(Launchable, sut(UseCache), "$country/$currency should be launchable")
        }
    }

    @Test
    fun `given lowercase currency code, when invoked, then matches case-insensitively`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("GB", "gbp"))

        assertEquals(Launchable, sut(UseCache))
    }

    @Test
    fun `given country outside the POS table and the all-countries flag on, when invoked, then Launchable`() =
        runTest {
            // There is no country to enforce and no currency to validate against.
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(true)
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("DE", "EUR"))

            assertEquals(Launchable, sut(UseCache))
        }

    @Test
    fun `given country outside the POS table and the flag off, when invoked, then SiteSettingsUnavailable`() =
        runTest {
            // Matches iOS, which reports an unsupported country as siteSettingsNotAvailable.
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("DE", "EUR"))

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.SiteSettingsUnavailable), result)
            // iOS treats siteSettingsNotAvailable as indeterminate, so the positive cache survives.
            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given a blank currency code with no cached positive, when invoked, then UnknownNoPositiveCache`() = runTest {
        // The settings response can omit the currency. That is an unknown, not a mismatch.
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("US", ""))

        val result = sut(UseCache)

        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, never()).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given a blank currency code with cached positive, when invoked, then Launchable`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("US", ""))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut(UseCache))
    }

    @Test
    fun `given site settings unavailable with no cached positive, when invoked, then UnknownNoPositiveCache`() =
        runTest {
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)
            whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(null))

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given site settings unavailable with cached positive, when invoked, then Launchable`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut(UseCache))
    }

    // --- WooCommerce plugin checks ---

    @Test
    fun `given the WooCommerce plugin is missing or inactive, when invoked, then WooCommercePluginNotFound`() =
        runTest {
            whenever(getWooCorePluginStatus(any()))
                .thenReturn(WooPosWooCorePluginStatus.NotInstalledOrInactive)

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.WooCommercePluginNotFound), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given the plugin state is unknown with no cached positive, when invoked, then UnknownNoPositiveCache`() =
        runTest {
            whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.CouldNotDetermine)

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given the plugin state is unknown with cached positive, when invoked, then Launchable`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.CouldNotDetermine)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut(UseCache))
    }

    // --- Version checks ---

    @Test
    fun `given unsupported WooCommerce version, when invoked, then UnsupportedWooCommerceVersion and clears cache`() =
        runTest {
            whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.5.0"))

            val result = sut(UseCache)

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedWooCommerceVersion), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given WC 9_6_0 (minimum supported), when invoked, then Launchable`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.6.0"))

        assertEquals(Launchable, sut(UseCache))
    }

    // --- Feature switch checks ---

    @Test
    fun `given WC below 10_0_0, when invoked, then the feature switch is not read`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.9.0"))

        assertEquals(Launchable, sut(UseCache))
        verify(isFeatureSwitchEnabled, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun `given the POS feature switch is off, when invoked, then FeatureSwitchDisabled and clears cache`() = runTest {
        whenever(isFeatureSwitchEnabled(any(), anyOrNull())).thenReturn(Result.success(false))

        val result = sut(UseCache)

        assertEquals(NotLaunchable(NonLaunchabilityReason.FeatureSwitchDisabled), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given the switch cannot be read with no cached positive, when invoked, then UnknownNoPositiveCache`() = runTest {
        whenever(isFeatureSwitchEnabled(any(), anyOrNull())).thenReturn(Result.failure(Exception()))

        val result = sut(UseCache)

        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, never()).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given the feature switch cannot be read with cached positive, when invoked, then Launchable`() = runTest {
        whenever(isFeatureSwitchEnabled(any(), anyOrNull())).thenReturn(Result.failure(Exception()))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut(UseCache))
    }

    // --- Force refresh paths ---

    @Test
    fun `given ForceRefresh, when invoked, then settings plugin and switch are all read remotely`() = runTest {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel))
            .thenReturn(WooResult(settings("US", "USD")))

        val result = sut(ForceRefresh)

        assertEquals(Launchable, result)
        verify(wooCommerceStore).fetchSiteGeneralSettings(siteModel)
        verify(getWooCorePluginStatus).invoke(WooPosLaunchabilityRefreshPolicy.ForceRefresh)
        verify(isFeatureSwitchEnabled).invoke(WooPosLaunchabilityRefreshPolicy.ForceRefresh, null)
    }

    @Test
    fun `given ForceRefresh and the plugin came from a report, when invoked, then the switch reuses it`() = runTest {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel))
            .thenReturn(WooResult(settings("US", "USD")))
        whenever(getWooCorePluginStatus(any()))
            .thenReturn(WooPosWooCorePluginStatus.Active("10.0.0", REPORT))

        assertEquals(Launchable, sut(ForceRefresh))

        // The report is the slowest WooCommerce endpoint; it is read once, not once per check.
        verify(isFeatureSwitchEnabled).invoke(WooPosLaunchabilityRefreshPolicy.ForceRefresh, REPORT)
    }

    // --- Refresh policy ---

    @Test
    fun `given UseCache, when invoked, then the policy is passed to the plugin and switch checks`() = runTest {
        assertEquals(Launchable, sut(UseCache))

        verify(getWooCorePluginStatus).invoke(WooPosLaunchabilityRefreshPolicy.UseCache)
        verify(isFeatureSwitchEnabled).invoke(WooPosLaunchabilityRefreshPolicy.UseCache, null)
        verify(wooCommerceStore, never()).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given UseCacheAndRefresh, when invoked, then the policy is passed to the plugin and switch checks`() =
        runTest {
            assertEquals(Launchable, sut(UseCacheAndRefresh))

            verify(getWooCorePluginStatus).invoke(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh)
            verify(isFeatureSwitchEnabled).invoke(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh, null)
            verify(wooCommerceStore, never()).fetchSiteGeneralSettings(any())
        }

    // --- Plan eligibility ---

    @Test
    fun `given site with free plan, when invoked, then not gated by plan`() = runTest {
        siteModel.planProductSlug = "woo_hosted_free_plan"

        val result = sut(UseCache)

        assertEquals(Launchable, result)
    }

    private companion object {
        val REPORT = WooPosSystemStatusReport(EnabledFeatures.Known(listOf("point_of_sale")))
    }
}
