package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.Launchable
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NonLaunchabilityReason
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability.NotLaunchable
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCanBeLaunchedInTabTest {

    private val appPrefs: AppPrefs = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getWooCorePluginStatus: WooPosGetWooCorePluginStatus = mock()
    private val isFeatureSwitchEnabled: WooPosIsFeatureSwitchEnabled = mock()

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
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("US", "USD"))
            whenever(getWooCorePluginStatus(any()))
                .thenReturn(WooPosWooCorePluginStatus.Active("10.0.0"))
            whenever(isFeatureSwitchEnabled(any())).thenReturn(Result.success(true))
        }
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(false)

        sut = WooPosCanBeLaunchedInTab(
            appPrefs = appPrefs,
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            getWooCorePluginStatus = getWooCorePluginStatus,
            isFeatureSwitchEnabled = isFeatureSwitchEnabled,
            wooPosLog = mock()
        )
    }

    private fun settings(countryCode: String, currencyCode: String) = Settings(
        currencyCode = currencyCode,
        currencyPosition = CurrencyPosition.LEFT,
        currencyThousandSeparator = ",",
        currencyDecimalSeparator = ".",
        currencyDecimalNumber = 2,
        countryCode = countryCode,
        stateCode = "",
        address = "",
        address2 = "",
        city = "",
        postalCode = "",
        couponsEnabled = true,
    )

    // --- Happy paths ---

    @Test
    fun `given valid conditions, when invoked, then return Launchable and set cache`() = runTest {
        val result = sut()
        assertEquals(Launchable, result)
        verify(appPrefs, times(1)).setPOSLaunchableForSite(eq(siteModel.id))
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

    // --- Currency checks ---

    @Test
    fun `given store currency does not match store country, when invoked, then NotLaunchable UnsupportedCurrency`() =
        runTest {
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("CA", "EUR"))

            val result = sut()

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedCurrency), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given every supported country with its currency, when invoked, then Launchable`() = runTest {
        val pairs = listOf(
            "US" to "USD", "PR" to "USD", "GB" to "GBP", "CA" to "CAD",
            "FI" to "EUR", "IE" to "EUR", "LU" to "EUR", "NL" to "EUR",
            "SG" to "SGD", "NZ" to "NZD", "AU" to "AUD",
        )

        pairs.forEach { (country, currency) ->
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings(country, currency))

            assertEquals(Launchable, sut(), "$country/$currency should be launchable")
        }
    }

    @Test
    fun `given lowercase currency code, when invoked, then matches case-insensitively`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("GB", "gbp"))

        assertEquals(Launchable, sut())
    }

    @Test
    fun `given country outside the POS table, when invoked, then currency is not validated`() = runTest {
        // Only reachable with the all-countries flag on. There is no currency to validate against.
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings("DE", "EUR"))

        assertEquals(Launchable, sut())
    }

    @Test
    fun `given site settings unavailable with no cached positive, when invoked, then UnknownNoPositiveCache`() =
        runTest {
            whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)
            whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(mock())

            val result = sut()

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given site settings unavailable with cached positive, when invoked, then Launchable`() = runTest {
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel)).thenReturn(mock())
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut())
    }

    // --- WooCommerce plugin checks ---

    @Test
    fun `given the WooCommerce plugin is missing or inactive, when invoked, then WooCommercePluginNotFound`() =
        runTest {
            whenever(getWooCorePluginStatus(any()))
                .thenReturn(WooPosWooCorePluginStatus.NotInstalledOrInactive)

            val result = sut()

            assertEquals(NotLaunchable(NonLaunchabilityReason.WooCommercePluginNotFound), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given the plugin state is unknown with no cached positive, when invoked, then UnknownNoPositiveCache`() =
        runTest {
            whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.CouldNotDetermine)

            val result = sut()

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
            verify(appPrefs, never()).clearPOSLaunchableForSite(any())
        }

    @Test
    fun `given the plugin state is unknown with cached positive, when invoked, then Launchable`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.CouldNotDetermine)
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut())
    }

    // --- Version checks ---

    @Test
    fun `given unsupported WooCommerce version, when invoked, then UnsupportedWooCommerceVersion and clears cache`() =
        runTest {
            whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.5.0"))

            val result = sut()

            assertEquals(NotLaunchable(NonLaunchabilityReason.UnsupportedWooCommerceVersion), result)
            verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
        }

    @Test
    fun `given WC 9_6_0 (minimum supported), when invoked, then Launchable`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.6.0"))

        assertEquals(Launchable, sut())
    }

    // --- Feature switch checks ---

    @Test
    fun `given WC below 10_0_0, when invoked, then the feature switch is not read`() = runTest {
        whenever(getWooCorePluginStatus(any())).thenReturn(WooPosWooCorePluginStatus.Active("9.9.0"))

        assertEquals(Launchable, sut())
        verify(isFeatureSwitchEnabled, never()).invoke(any())
    }

    @Test
    fun `given the POS feature switch is off, when invoked, then FeatureSwitchDisabled and clears cache`() = runTest {
        whenever(isFeatureSwitchEnabled(any())).thenReturn(Result.success(false))

        val result = sut()

        assertEquals(NotLaunchable(NonLaunchabilityReason.FeatureSwitchDisabled), result)
        verify(appPrefs, times(1)).clearPOSLaunchableForSite(eq(siteModel.id))
    }

    @Test
    fun `given the switch cannot be read with no cached positive, when invoked, then UnknownNoPositiveCache`() = runTest {
        whenever(isFeatureSwitchEnabled(any())).thenReturn(Result.failure(Exception()))

        val result = sut()

        assertEquals(NotLaunchable(NonLaunchabilityReason.UnknownNoPositiveCache), result)
        verify(appPrefs, never()).clearPOSLaunchableForSite(any())
    }

    @Test
    fun `given the feature switch cannot be read with cached positive, when invoked, then Launchable`() = runTest {
        whenever(isFeatureSwitchEnabled(any())).thenReturn(Result.failure(Exception()))
        whenever(appPrefs.isPOSLaunchableForSite(eq(siteModel.id))).thenReturn(true)

        assertEquals(Launchable, sut())
    }

    // --- Force refresh paths ---

    @Test
    fun `given forceRefresh true, when invoked, then settings plugin and switch are all read remotely`() = runTest {
        whenever(wooCommerceStore.fetchSiteGeneralSettings(siteModel))
            .thenReturn(WooResult(settings("US", "USD")))

        val result = sut(forceRefresh = true)

        assertEquals(Launchable, result)
        verify(wooCommerceStore).fetchSiteGeneralSettings(siteModel)
        verify(getWooCorePluginStatus).invoke(true)
        verify(isFeatureSwitchEnabled).invoke(true)
    }

    // --- Plan eligibility ---

    @Test
    fun `given site with free plan, when invoked, then not gated by plan`() = runTest {
        siteModel.planProductSlug = "woo_hosted_free_plan"

        val result = sut()

        assertEquals(Launchable, result)
    }
}
