package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.ui.woopos.common.util.WooPosCouldNotDetermineValueException
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
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
class WooPosTabShouldBeVisibleTest {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val isCountryAllowed: WooPosIsCountryAllowed = mock {
        on { invoke() } doReturn true
    }

    private lateinit var sut: WooPosTabShouldBeVisible
    private lateinit var siteModel: SiteModel

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Before
    fun setup() {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(isScreenSizeAllowed()).thenReturn(true)
        whenever(appPrefs.isPOSTabVisibleForSite(any())).thenReturn(false)

        sut = WooPosTabShouldBeVisible(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            isScreenSizeAllowed = isScreenSizeAllowed,
            isCountryAllowed = isCountryAllowed,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given tablet, when invoked with forceRefresh, then return success true`() = runTest {
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given screen size not allowed, when invoked with forceRefresh, then return success false`() = runTest {
        whenever(isScreenSizeAllowed()).thenReturn(false)
        val r = sut(forceRefresh = true)
        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
    }

    @Test
    fun `given null site, when invoked with forceRefresh, then return failure unknown`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val r = sut(forceRefresh = true)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is WooPosCouldNotDetermineValueException)
    }

    @Test
    fun `given cached value is true, when invoked without forceRefresh, then return cached value`() = runTest {
        whenever(appPrefs.isPOSTabVisibleForSite(siteModel.id)).thenReturn(true)

        val r = sut(forceRefresh = false)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
    }

    @Test
    fun `given successful check, when invoked with forceRefresh, then cache positive result`() = runTest {
        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow())
        verify(appPrefs).setPOSTabVisibilityForSite(siteModel.id)
    }

    @Test
    fun `given screen size not allowed, when invoked with forceRefresh, then clear cache`() = runTest {
        whenever(isScreenSizeAllowed()).thenReturn(false)

        val r = sut(forceRefresh = true)

        assertTrue(r.isSuccess)
        assertFalse(r.getOrThrow())
        verify(appPrefs).clearPOSTabVisibilityForSite(siteModel.id)
    }

    @Test
    fun `given country is not allowed, when invoked with forceRefresh, then return success false and clear cache`() =
        runTest {
            whenever(isCountryAllowed()).thenReturn(false)

            val r = sut(forceRefresh = true)

            assertTrue(r.isSuccess)
            assertFalse(r.getOrThrow())
            verify(appPrefs).clearPOSTabVisibilityForSite(siteModel.id)
        }
}
