package com.woocommerce.android.ui.woopos

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosIsEnabledTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() }.thenReturn("9.6.0")
    }

    private lateinit var sut: WooPosIsEnabled

    @Before
    fun setup() = testBlocking {
        val siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isScreenSizeAllowed()).thenReturn(true)
        whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(true)
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)

        sut = WooPosIsEnabled(
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            isScreenSizeAllowed = isScreenSizeAllowed,
            isRemoteFeatureFlagEnabled = isRemoteFeatureFlagEnabled,
            getWooCoreVersion = getWooCoreVersion,
        )
    }

    @Test
    fun `given feature flag enabled screen size allowed supported country and currency, when invoked, then return true`() =
        testBlocking {
            whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(true)
            whenever(isScreenSizeAllowed()).thenReturn(true)

            assertTrue(sut())
        }

    @Test
    fun `given feature flag disabled, when invoked, then return false`() = testBlocking {
        whenever(isRemoteFeatureFlagEnabled.invoke(WOO_POS)).thenReturn(false)
        assertFalse(sut())
    }

    @Test
    fun `given unsupported country, when invoked, then return false`() = testBlocking {
        val result = buildSiteSettings(countryCode = "CA", currencyCode = "USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given unsupported currency, when invoked, then return false`() = testBlocking {
        val result = buildSiteSettings(currencyCode = "CAD", countryCode = "US")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given uk country and pounds, when invoked, then return true`() = testBlocking {
        val result = buildSiteSettings(countryCode = "GB", currencyCode = "GBP")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(result)
        assertTrue(sut())
    }

    @Test
    fun `given uk country and usd, when invoked, then return false`() = testBlocking {
        val result = buildSiteSettings(countryCode = "GB", currencyCode = "USD")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given us country and pounds, when invoked, then return false`() = testBlocking {
        val result = buildSiteSettings(countryCode = "US", currencyCode = "GBP")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(result)
        assertFalse(sut())
    }

    @Test
    fun `given woo version 9_5_0, when invoked, then return false`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("9.5.0")
        assertFalse(sut())
    }

    @Test
    fun `given woo version 9_6_0, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("9.6.0")
        assertTrue(sut())
    }

    @Test
    fun `given woo version 9_6_0_1, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("9.6.0.1")
        assertTrue(sut())
    }

    @Test
    fun `given woo version 10_0_1, when invoked, then return true`() = testBlocking {
        whenever(getWooCoreVersion.invoke()).thenReturn("10.0.1")
        assertTrue(sut())
    }

    private fun buildSiteSettings(
        countryCode: String = "US",
        currencyCode: String = "USD"
    ) = mock<WCSettingsModel> {
        on { this.countryCode }.thenReturn(countryCode)
        on { this.currencyCode }.thenReturn(currencyCode)
    }
}
