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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosTabShouldBeVisibleTest : BaseUnitTest() {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
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

        sut = WooPosTabShouldBeVisible(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            isScreenSizeAllowed = isScreenSizeAllowed,
            featureFlagRepository = featureFlagRepository,
            ciabSiteGateKeeper = ciabSiteGateKeeper,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given feature flag enabled and tablet, when invoked with forceRefresh, then return success true`() = testBlocking {
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
    fun `given feature unsupported for CIAB site, when invoked with forceRefresh, then return success false`() = testBlocking {
        whenever(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.POS)).thenReturn(true)

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given cached value is true, when invoked without forceRefresh, then return cached value`() = testBlocking {
        whenever(appPrefs.isPOSTabVisibleForSite(siteModel.id)).thenReturn(true)

        val r = sut(forceRefresh = false)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given successful check, when invoked with forceRefresh, then cache positive result`() = testBlocking {
        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
        verify(appPrefs).setPOSTabVisibilityForSite(siteModel.id)
    }

    @Test
    fun `given feature flag off, when invoked with forceRefresh, then clear cache`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS)).thenReturn(false)

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
        verify(appPrefs).clearPOSTabVisibilityForSite(siteModel.id)
    }
}
