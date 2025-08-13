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

    private lateinit var sut: WooPosCanBeLaunchedInTab
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() = testBlocking {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(fetchWooCoreVersion()).thenReturn("10.0.0")
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(siteSettings))
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.success(true))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            getWooCoreCachedVersion = getWooCoreVersion,
            fetchWooCoreVersion = fetchWooCoreVersion,
            wooCommerceStore = wooCommerceStore,
            isRemotelyEnabled = isRemotelyEnabled,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given valid conditions, when invoked, then return Launchable`() = testBlocking {
        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given no site selected, when invoked, then return NotLaunchable with NoSiteSelected`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.NoSiteSelected), result)
    }

    @Test
    fun `given unsupported WooCommerce version, when invoked, then return NotLaunchable with UnsupportedWooCommerceVersion`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.5.0")
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedWooCommerceVersion), result)
    }

    @Test
    fun `given feature switch supported but remotely disabled, when invoked, then return NotLaunchable with FeatureSwitchDisabled`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.success(false))
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.FeatureSwitchDisabled), result)
    }

    @Test
    fun `given null site settings and no cached positive, when invoked, then return NotLaunchable UnknownNoPositiveCache`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
    }

    @Test
    fun `given unsupported currency, when invoked, then return NotLaunchable with UnsupportedCurrency`() = testBlocking {
        val siteSettings = buildSiteSettings(currencyCode = "eur")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
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
    fun `given uk country and usd, when invoked, then return Not Launchable`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "GB", currencyCode = "USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
    }

    @Test
    fun `given us country and pounds, when invoked, then return Not Launchable`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "US", currencyCode = "GBP")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
    }

    @Test
    fun `given site settings missing but fetched successfully, when invoked, then return Launchable`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(currencyCode = "usd")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val result = sut()
        assertEquals(Launchable, result)
    }

    @Test
    fun `given site settings missing and fetched with unsupported currency, when invoked, then return NotLaunchable with UnsupportedCurrency`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(currencyCode = "eur")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
    }

    @Test
    fun `given forceRefresh true with valid data, when invoked, then return Launchable`() = testBlocking {
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with no cached positive, when invoked, then NotLaunchable WooCommercePluginNotFound`() = testBlocking {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        val result = sut(forceRefresh = true)
        assertEquals(NotLaunchable(NonLaunchabilityReason.WooCommercePluginNotFound), result)
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with cached positive, when invoked, then Launchable`() = testBlocking {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    @Test
    fun `given forceRefresh true and fetched site settings is null with no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache`() = testBlocking {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        val result = sut(forceRefresh = true)
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
    }

    @Test
    fun `given forceRefresh true and fetched site settings is null with cached positive, when invoked, then Launchable`() = testBlocking {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    @Test
    fun `given supported country and currency from cache, when invoked, then sets POS launchable for site`() = testBlocking {
        val result = sut()
        assertEquals(Launchable, result)

        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given forceRefresh true with valid data, when invoked, then sets POS launchable for site`() = testBlocking {
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)

        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

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
