package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPOSIsRemotelyEnabled
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Launchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NotLaunchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Reason
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() }.thenReturn("9.6.0")
    }
    private lateinit var sut: WooPosCanBeLaunchedInTab

    @Before
    fun setup() = testBlocking {
        val siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)
        whenever(isRemotelyEnabled.invoke()).thenReturn(true)

        sut = WooPosCanBeLaunchedInTab(
            selectedSite = selectedSite,
            getWooCoreVersion = getWooCoreVersion,
            wooCommerceStore = wooCommerceStore,
            isRemotelyEnabled = isRemotelyEnabled
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
        assertEquals(NotLaunchable(Reason.NoSiteSelected), result)
    }

    @Test
    fun `given unsupported WooCommerce version, when invoked, then return NotLaunchable with UnsupportedWooCommerceVersion`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.5.0") // lower than 9.6.0
        val result = sut()
        assertEquals(NotLaunchable(Reason.UnsupportedWooCommerceVersion), result)
    }

    @Test
    fun `given feature switch supported but remotely disabled, when invoked, then return NotLaunchable with FeatureSwitchDisabled`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(
            isRemotelyEnabled.invoke()
        ).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            selectedSite = selectedSite,
            getWooCoreVersion = getWooCoreVersion,
            wooCommerceStore = wooCommerceStore,
            isRemotelyEnabled = isRemotelyEnabled
        )

        val result = sut()
        assertEquals(NotLaunchable(Reason.FeatureSwitchDisabled), result)
    }

    @Test
    fun `given null site settings, when invoked, then return NotLaunchable with SiteSettingsUnavailable`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(null))
        val result = sut()
        assertEquals(NotLaunchable(Reason.SiteSettingsUnavailable), result)
    }

    @Test
    fun `given unsupported currency, when invoked, then return NotLaunchable with UnsupportedCurrency`() = testBlocking {
        val siteSettings = buildSiteSettings(currencyCode = "eur")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        val result = sut()
        assertEquals(NotLaunchable(Reason.UnsupportedCurrency), result)
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
        assertEquals(NotLaunchable(Reason.UnsupportedCurrency), result)
    }

    private fun buildSiteSettings(currencyCode: String = "usd") =
        WCSettingsTestUtils.generateSettings(
            siteId = LocalOrRemoteId.LocalId(1)
        ).copy(
            currencyCode = currencyCode
        )
}
