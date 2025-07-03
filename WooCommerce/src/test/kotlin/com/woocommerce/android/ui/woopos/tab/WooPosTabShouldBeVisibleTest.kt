package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosTabShouldBeVisibleTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled = mock()

    private lateinit var sut: WooPosTabShouldBeVisible

    @Before
    fun setup() = testBlocking {
        val siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(true)
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(siteSettings)

        sut = WooPosTabShouldBeVisible(
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            isRemoteFeatureFlagEnabled = isRemoteFeatureFlagEnabled
        )
    }

    @Test
    fun `given feature flag enabled and supported country, when invoked, then return true`() = testBlocking {
        assertTrue(sut())
    }

    @Test
    fun `given feature flag disabled, when invoked, then return false`() = testBlocking {
        whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(false)
        assertFalse(sut())
    }

    @Test
    fun `given null site, when invoked, then return false`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        assertFalse(sut())
    }

    @Test
    fun `given unsupported country, when invoked, then return false`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(siteSettings)
        assertFalse(sut())
    }

    @Test
    fun `given null local site settings, when invoked, then fetch remote settings`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(countryCode = "us")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        sut()

        verify(wooCommerceStore).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given remote settings with supported country, when invoked, then return true`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(countryCode = "gb")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        assertTrue(sut())
    }

    @Test
    fun `given remote settings with unsupported country, when invoked, then return false`() = testBlocking {
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        val fetchedSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        assertFalse(sut())
    }

    private fun buildSiteSettings(countryCode: String = "us") =
        WCSettingsTestUtils.generateSettings(
            siteId = LocalOrRemoteId.LocalId(1)
        ).copy(
            countryCode = countryCode
        )
}
