package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock {
        on { isFeatureUnsupported(CIABAffectedFeature.POS) } doReturn false
    }

    private lateinit var sut: WooPosTabShouldBeVisible
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() = testBlocking {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isScreenSizeAllowed()).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS)).thenReturn(true)
        whenever(featureFlagRepository.awaitRemoteFlagsLoaded()).thenReturn(Unit)
        whenever(appPrefs.isPOSTabVisibleForSite(any())).thenReturn(false)
        val siteSettings = buildSiteSettings()
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(WooResult(siteSettings))

        sut = WooPosTabShouldBeVisible(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            isScreenSizeAllowed = isScreenSizeAllowed,
            wooCommerceStore = wooCommerceStore,
            featureFlagRepository = featureFlagRepository,
            ciabSiteGateKeeper = ciabSiteGateKeeper,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given feature flag enabled and supported country, when invoked with forceRefresh, then return success true`() = testBlocking {
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given feature flag disabled, when invoked with forceRefresh, then return success false`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS)).thenReturn(false)
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given screen size not allowed, when invoked with forceRefresh, then return success false`() = testBlocking {
        whenever(isScreenSizeAllowed()).thenReturn(false)
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given null site, when invoked with forceRefresh, then return failure unknown`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val r = sut(forceRefresh = true)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is WooPosCouldNotDetermineValueException)
    }

    @Test
    fun `given unsupported country, when invoked with forceRefresh, then return success false`() = testBlocking {
        val siteSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(siteSettings))
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given forceRefresh true, when invoked, then always fetch remote settings`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "us")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        sut(forceRefresh = true)

        verify(wooCommerceStore).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given fetched settings with supported country, when invoked with forceRefresh, then return success true`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "gb")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given fetched settings with unsupported country, when invoked with forceRefresh, then return success false`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given feature unsupported for CIAB site, when invoked with forceRefresh, then return success false`() = testBlocking {
        whenever(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.POS)).thenReturn(true)

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given cached value is true, when invoked without forceRefresh, then return cached value without remote call`() = testBlocking {
        whenever(appPrefs.isPOSTabVisibleForSite(siteModel.id)).thenReturn(true)

        val r = sut(forceRefresh = false)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
        verify(wooCommerceStore, never()).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given cached value is false, when invoked without forceRefresh, then fetch from remote`() = testBlocking {
        whenever(appPrefs.isPOSTabVisibleForSite(siteModel.id)).thenReturn(false)
        val fetchedSettings = buildSiteSettings(countryCode = "us")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut(forceRefresh = false)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
        verify(wooCommerceStore).fetchSiteGeneralSettings(any())
    }

    @Test
    fun `given successful check with supported country, when invoked with forceRefresh, then cache positive result`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "us")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
        verify(appPrefs).setPOSTabVisibilityForSite(siteModel.id)
    }

    @Test
    fun `given check fails with unsupported country, when invoked with forceRefresh, then clear cache`() = testBlocking {
        val fetchedSettings = buildSiteSettings(countryCode = "ca")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(any())).thenReturn(WooResult(fetchedSettings))

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
        verify(appPrefs).clearPOSTabVisibilityForSite(siteModel.id)
    }

    private fun buildSiteSettings(countryCode: String = "us") =
        WCSettingsTestUtils.generateSettings(
            siteId = LocalOrRemoteId.LocalId(1)
        ).copy(
            countryCode = countryCode
        )
}
