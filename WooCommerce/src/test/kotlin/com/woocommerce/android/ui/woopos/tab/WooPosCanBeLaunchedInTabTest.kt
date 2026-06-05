package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPOSIsRemotelyEnabled
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Launchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NonLaunchabilityReason
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NotLaunchable
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCanBeLaunchedInTabTest : BaseUnitTest() {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val fetchWooCoreVersion: FetchActiveWCPluginVersion = mock()
    private val supportedCountries: WooPosSupportedCountries = mock()

    private lateinit var sut: WooPosCanBeLaunchedInTab
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() = testBlocking {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        // Default: WC 10.0.0, settings available and supported, remote enabled, no cached positive
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(fetchWooCoreVersion()).thenReturn("10.0.0")

        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(siteSettings))

        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.success(true))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        whenever(supportedCountries.supportedCountryCurrencyPairs()).thenReturn(listOf("us" to "usd", "gb" to "gbp"))

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            getWooCoreCachedVersion = getWooCoreVersion,
            fetchWooCoreVersion = fetchWooCoreVersion,
            wooCommerceStore = wooCommerceStore,
            isRemotelyEnabled = isRemotelyEnabled,
            supportedCountries = supportedCountries,
            wooPosLog = mock()
        )
    }

    // --- Happy paths ---

    @Test
    fun `given valid conditions, when invoked, then return Launchable and set cache`() = testBlocking {
        val result = sut()
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given uk country and pounds, when invoked, then return Launchable`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "GB", currencyCode = "GBP")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given us country and dollars, when invoked, then return Launchable`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "US", currencyCode = "USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given site settings missing but fetched successfully, when invoked, then return Launchable`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(currencyCode = "usd")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))
        val result = sut()
        assertEquals(Launchable, result)
    }

    // --- No site ---

    @Test
    fun `given no site selected, when invoked, then NotLaunchable NoSiteSelected and no prefs touched`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.NoSiteSelected), result)
        verify(appPrefs, times(0)).setPOSLaunchableForSite(any())
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    // --- Version checks ---

    @Test
    fun `given unsupported WooCommerce version, when invoked, then NotLaunchable UnsupportedWooCommerceVersion and clears cache`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.5.0")
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedWooCommerceVersion), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    // Feature-switch unsupported (version >= 9_6_0 but  switch threshold not met)
    @Test
    fun `given WC 9_6_0 (switch unsupported) and supported settings, when invoked, then Launchable`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.6.0")
        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given WC 9_6_0 (switch unsupported) and unsupported currency, when invoked, then NotLaunchable UnsupportedCurrency and clears cache`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.6.0")
        val bad = buildSiteSettings(currencyCode = "EUR")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(bad)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    // --- Feature switch checks ---

    @Test
    fun `given feature switch supported but remotely disabled, when invoked, then NotLaunchable FeatureSwitchDisabled and clears cache`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.success(false))
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.FeatureSwitchDisabled), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given remote check failure and cached positive, when invoked, then Launchable`() = testBlocking {
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.failure(RuntimeException("boom")))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)
        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given remote check failure and no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache and does not clear`() = testBlocking {
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.failure(RuntimeException("boom")))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    // --- Site settings fallback ---

    @Test
    fun `given null site settings and no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache and does not clear`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given site settings missing and fetched with unsupported currency, when invoked, then NotLaunchable UnsupportedCurrency and clears cache`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(currencyCode = "eur")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    // --- Currency matrix ---

    @Test
    fun `given uk country and usd, when invoked, then NotLaunchable UnsupportedCurrency and clears cache`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "GB", currencyCode = "USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given us country and pounds, when invoked, then NotLaunchable UnsupportedCurrency and clears cache`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "US", currencyCode = "GBP")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    // --- Force refresh paths ---

    @Test
    fun `given forceRefresh true with valid data, when invoked, then Launchable and set cache`() = testBlocking {
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache and does not clear`() = testBlocking {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        val result = sut(forceRefresh = true)
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with cached positive, when invoked, then Launchable`() = testBlocking {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    @Test
    fun `given forceRefresh true and fetched site settings is null with no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache and does not clear`() = testBlocking {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        val result = sut(forceRefresh = true)
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given forceRefresh true and fetched site settings is null with cached positive, when invoked, then Launchable`() = testBlocking {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    // --- CIAB plan eligibility ---

    @Test
    fun `given CIAB site with free plan, when invoked, then return CiabPlanUpgradeRequired`() = testBlocking {
        siteModel.setIsGardenSite(true)
        siteModel.gardenName = SiteModel.CIAB_GARDEN_NAME
        siteModel.planProductSlug = "woo_hosted_free_plan"

        val result = sut()

        assertEquals(NotLaunchable(NonLaunchabilityReason.CiabPlanUpgradeRequired), result)
    }

    @Test
    fun `given CIAB site with pro monthly plan, when invoked, then return Launchable`() = testBlocking {
        siteModel.setIsGardenSite(true)
        siteModel.gardenName = SiteModel.CIAB_GARDEN_NAME
        siteModel.planProductSlug = "woo_hosted_pro_plan_monthly"

        val result = sut()

        assertEquals(Launchable, result)
    }

    @Test
    fun `given non-CIAB site with free plan, when invoked, then not blocked by CIAB check`() = testBlocking {
        siteModel.setIsGardenSite(false)
        siteModel.planProductSlug = "woo_hosted_free_plan"

        val result = sut()

        assertEquals(Launchable, result)
    }

    // --- Supported countries dynamic list ---

    @Test
    fun `given IE country and EUR currency and primary flag on, when invoked, then return Launchable`() = testBlocking {
        whenever(supportedCountries.supportedCountryCurrencyPairs())
            .thenReturn(listOf("us" to "usd", "gb" to "gbp", "ie" to "eur"))
        val siteSettings = buildSiteSettings(countryCode = "IE", currencyCode = "EUR")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)

        assertEquals(Launchable, sut())
    }

    @Test
    fun `given IE country and EUR currency and primary flag off, when invoked, then return UnsupportedCurrency`() = testBlocking {
        whenever(supportedCountries.supportedCountryCurrencyPairs())
            .thenReturn(listOf("us" to "usd", "gb" to "gbp"))
        val siteSettings = buildSiteSettings(countryCode = "IE", currencyCode = "EUR")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)

        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
    }

    @Test
    fun `given AU country and AUD currency and AU flag on, when invoked, then return Launchable`() = testBlocking {
        whenever(supportedCountries.supportedCountryCurrencyPairs())
            .thenReturn(
                listOf(
                    "us" to "usd",
                    "gb" to "gbp",
                    "ie" to "eur",
                    "nl" to "eur",
                    "sg" to "sgd",
                    "nz" to "nzd",
                    "fi" to "eur",
                    "lu" to "eur",
                    "au" to "aud",
                )
            )
        val siteSettings = buildSiteSettings(countryCode = "AU", currencyCode = "AUD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)

        assertEquals(Launchable, sut())
    }

    // ---

    private fun buildSiteSettings(
        countryCode: String = "US",
        currencyCode: String = "USD"
    ) = WCSettingsTestUtils.generateSettings(
        siteId = LocalOrRemoteId.LocalId(1)
    ).copy(
        countryCode = countryCode,
        currencyCode = currencyCode
    )
}
