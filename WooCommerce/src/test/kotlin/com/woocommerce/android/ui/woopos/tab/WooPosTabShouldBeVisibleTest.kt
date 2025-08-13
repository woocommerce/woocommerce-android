package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
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
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled = mock()

    private lateinit var sut: WooPosTabShouldBeVisible
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() = testBlocking {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isScreenSizeAllowed()).thenReturn(true)
        whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(true)
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(siteSettings))

        sut = WooPosTabShouldBeVisible(
            selectedSite = selectedSite,
            isScreenSizeAllowed = isScreenSizeAllowed,
            wooCommerceStore = wooCommerceStore,
            isRemoteFeatureFlagEnabled = isRemoteFeatureFlagEnabled,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given feature flag enabled and supported country, when invoked, then return success true`() = testBlocking {
        val r = sut()
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given feature flag disabled, when invoked, then return success true (tab visible anyway)`() = testBlocking {
        whenever(isRemoteFeatureFlagEnabled(WOO_POS)).thenReturn(false)
        val r = sut()
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given screen size not allowed, when invoked, then return success false`() = testBlocking {
        whenever(isScreenSizeAllowed()).thenReturn(false)
        val r = sut()
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given null site, when invoked, then return failure unknown`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val r = sut()
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is WooPosCouldNotDetermineValueException)
    }

    @Test
    fun `given unsupported country, when invoked, then return success false`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(siteSettings))
        val r = sut()
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `when invoked, then always fetch remote settings`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "us")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        sut()

        verify(wooCommerceStore).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given fetched settings with supported country, when invoked, then return success true`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "gb")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut()
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given fetched settings with unsupported country, when invoked, then return success false`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut()
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    private fun buildSiteSettings(countryCode: String = "us") =
        WCSettingsTestUtils.generateSettings(
            siteId = LocalOrRemoteId.LocalId(1)
        ).copy(
            countryCode = countryCode
        )
}
