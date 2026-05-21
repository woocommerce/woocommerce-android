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
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCanBeLaunchedInTabTest : BaseUnitTest() {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val isRemotelyEnabled: WooPOSIsRemotelyEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val fetchWooCoreVersion: FetchActiveWCPluginVersion = mock()

    private lateinit var sut: WooPosCanBeLaunchedInTab
    private lateinit var siteModel: SiteModel

    @Before
    fun setup() = testBlocking {
        siteModel = SiteModel().also { it.id = 1 }
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        // Default: WC 10.0.0 (feature-switch threshold met), remotely enabled, no cached positive.
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(fetchWooCoreVersion()).thenReturn("10.0.0")
        whenever(isRemotelyEnabled.invoke(any())).thenReturn(Result.success(true))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            getWooCoreCachedVersion = getWooCoreVersion,
            fetchWooCoreVersion = fetchWooCoreVersion,
            isRemotelyEnabled = isRemotelyEnabled,
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
    fun `given any country code, when invoked, then return Launchable regardless of country`() = testBlocking {
        // No country gate exists any more — POS is available worldwide; card-payment
        // gating moved to the POS UI. This test guards against regression.
        assertEquals(Launchable, sut())
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

    // Feature-switch unsupported (version >= 9_6_0 but switch threshold not met)
    @Test
    fun `given WC 9_6_0 (switch unsupported), when invoked, then Launchable`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("9.6.0")
        val result = sut()
        assertEquals(Launchable, result)
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
}
