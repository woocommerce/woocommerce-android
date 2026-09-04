package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosGetWooCorePluginStatusTest {

    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val wooCommerceStore: WooCommerceStore = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sut = WooPosGetWooCorePluginStatus(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
    )

    private fun plugin(name: String, version: String, isActive: Boolean) = SitePluginModel(
        siteId = LocalId(site.id),
        name = name,
        version = version,
        slug = name.substringAfterLast('/'),
        authorName = "",
        isActive = isActive,
    )

    @Test
    fun `given the active Woo plugin is cached, when invoked, then returns Active with its version`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("woocommerce/woocommerce", "10.1.0", isActive = true)))

        assertThat(sut(forceRefresh = false))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
    }

    @Test
    fun `given the Woo plugin is installed but inactive, when invoked, then returns NotInstalledOrInactive`() =
        runTest {
            whenever(wooCommerceStore.getSitePlugins(site))
                .thenReturn(listOf(plugin("woocommerce/woocommerce", "10.1.0", isActive = false)))

            assertThat(sut(forceRefresh = false))
                .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
        }

    @Test
    fun `given other plugins but no Woo plugin, when invoked, then returns NotInstalledOrInactive`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("jetpack/jetpack", "13.0", isActive = true)))

        assertThat(sut(forceRefresh = false))
            .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
    }

    @Test
    fun `given nothing has been synced for the site, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(emptyList())

        assertThat(sut(forceRefresh = false))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given no site is selected, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut(forceRefresh = false))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given forceRefresh and the fetch fails, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(wooCommerceStore.fetchSitePlugins(site))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)))

        assertThat(sut(forceRefresh = true))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given forceRefresh and the fetch succeeds, when invoked, then returns the fetched state`() = runTest {
        whenever(wooCommerceStore.fetchSitePlugins(site))
            .thenReturn(WooResult(listOf(plugin("woocommerce/woocommerce", "10.2.0", isActive = true))))

        assertThat(sut(forceRefresh = true))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.2.0"))
    }

    @Test
    fun `given a plugin name without a directory prefix, when invoked, then it still matches Woo core`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("woocommerce", "10.1.0", isActive = true)))

        assertThat(sut(forceRefresh = false))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
    }
}
