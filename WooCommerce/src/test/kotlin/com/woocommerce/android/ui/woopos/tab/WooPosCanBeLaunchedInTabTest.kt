package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Launchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NonLaunchabilityReason
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NotLaunchable
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCanBeLaunchedInTabTest {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val fetchWooCoreVersion: FetchActiveWCPluginVersion = mock()

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
            whenever(getWooCoreVersion()).thenReturn("10.0.0")
            whenever(fetchWooCoreVersion()).thenReturn("10.0.0")
        }
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            getWooCoreCachedVersion = getWooCoreVersion,
            fetchWooCoreVersion = fetchWooCoreVersion,
            wooPosLog = mock()
        )
    }

    // --- Happy paths ---

    @Test
    fun `given valid conditions, when invoked, then return Launchable and set cache`() = runTest {
        val result = sut()
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given any country code, when invoked, then return Launchable regardless of country`() = runTest {
        // No country gate exists any more — POS is available worldwide; card-payment
        // gating moved to the POS UI. This test guards against regression.
        assertEquals(Launchable, sut())
    }

    // --- No site ---

    @Test
    fun `given no site selected, when invoked, then NotLaunchable NoSiteSelected and no prefs touched`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.NoSiteSelected), result)
        verify(appPrefs, times(0)).setPOSLaunchableForSite(any())
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    // --- Version checks ---

    @Test
    fun `given unsupported WooCommerce version, when invoked, then NotLaunchable UnsupportedWooCommerceVersion and clears cache`() = runTest {
        whenever(getWooCoreVersion()).thenReturn("9.5.0")
        val result = sut()
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedWooCommerceVersion), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given WC 9_6_0 (minimum supported), when invoked, then Launchable`() = runTest {
        whenever(getWooCoreVersion()).thenReturn("9.6.0")
        val result = sut()
        assertEquals(Launchable, result)
    }

    // --- Force refresh paths ---

    @Test
    fun `given forceRefresh true with valid data, when invoked, then Launchable and set cache`() = runTest {
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with no cached positive, when invoked, then NotLaunchable UnknownNoPositiveCache and does not clear`() = runTest {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)
        val result = sut(forceRefresh = true)
        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, times(0)).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given forceRefresh true and fetchWooCoreVersion returns null with cached positive, when invoked, then Launchable`() = runTest {
        whenever(fetchWooCoreVersion()).thenReturn(null)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)
        val result = sut(forceRefresh = true)
        assertEquals(Launchable, result)
    }

    // --- Plan eligibility ---

    @Test
    fun `given site with free plan, when invoked, then not gated by plan`() = runTest {
        siteModel.planProductSlug = "woo_hosted_free_plan"

        val result = sut()

        assertEquals(Launchable, result)
    }
}
