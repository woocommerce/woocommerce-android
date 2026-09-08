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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.EnabledFeatures
import org.wordpress.android.fluxc.store.WooCommerceStore.SitePluginsAndFeatures

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

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
    }

    @Test
    fun `given the Woo plugin is installed but inactive, when invoked, then returns NotInstalledOrInactive`() =
        runTest {
            whenever(wooCommerceStore.getSitePlugins(site))
                .thenReturn(listOf(plugin("woocommerce/woocommerce", "10.1.0", isActive = false)))

            assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
                .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
        }

    @Test
    fun `given other plugins but no Woo plugin, when invoked, then returns NotInstalledOrInactive`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("jetpack/jetpack", "13.0", isActive = true)))

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
    }

    @Test
    fun `given nothing has been synced for the site, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(emptyList())

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given no site is selected, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given ForceRefresh and the fetch fails, when invoked, then returns CouldNotDetermine`() = runTest {
        whenever(wooCommerceStore.fetchSitePluginsAndSettings(site))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)))

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh))
            .isEqualTo(WooPosWooCorePluginStatus.CouldNotDetermine)
    }

    @Test
    fun `given ForceRefresh and the fetch succeeds, when invoked, then returns the fetched state`() = runTest {
        whenever(wooCommerceStore.fetchSitePluginsAndSettings(site)).thenReturn(
            WooResult(
                SitePluginsAndFeatures(
                    plugins = listOf(plugin("woocommerce/woocommerce", "10.2.0", isActive = true)),
                    enabledFeatures = KNOWN_FEATURES,
                )
            )
        )

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh))
            .isEqualTo(
                WooPosWooCorePluginStatus.Active("10.2.0", WooPosSystemStatusReport(KNOWN_FEATURES))
            )
    }

    @Test
    fun `given ForceRefresh and the report lists no Woo plugin, when invoked, then NotInstalledOrInactive`() =
        runTest {
            // The report answered, so the plugin's absence is the site's actual state, not an unknown.
            whenever(wooCommerceStore.fetchSitePluginsAndSettings(site))
                .thenReturn(
                    WooResult(
                        SitePluginsAndFeatures(plugins = emptyList(), enabledFeatures = EnabledFeatures.Unknown)
                    )
                )

            assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh))
                .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
        }

    @Test
    fun `given ForceRefresh and a report without enabled_features, when invoked, then the report is still carried`() =
        runTest {
            whenever(wooCommerceStore.fetchSitePluginsAndSettings(site)).thenReturn(
                WooResult(
                    SitePluginsAndFeatures(
                        plugins = listOf(plugin("woocommerce/woocommerce", "10.2.0", isActive = true)),
                        enabledFeatures = EnabledFeatures.Unknown,
                    )
                )
            )

            // Not Active("10.2.0", null): the report answered, and that has to stay distinguishable
            // from a state read out of local data, which is what a null report means.
            assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh))
                .isEqualTo(
                    WooPosWooCorePluginStatus.Active("10.2.0", WooPosSystemStatusReport(EnabledFeatures.Unknown))
                )
        }

    @Test
    fun `given a plugin name without a directory prefix, when invoked, then it still matches Woo core`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("woocommerce", "10.1.0", isActive = true)))

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
    }

    @Test
    fun `given an inactive plugin sorts before the active Woo plugin, when invoked, then returns Active`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site)).thenReturn(
            listOf(
                plugin("a-backup/woocommerce", "9.0.0", isActive = false),
                plugin("woocommerce/woocommerce", "10.1.0", isActive = true),
            )
        )

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCache))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
    }

    @Test
    fun `given ForceRefresh and the route is missing, when invoked, then returns NotInstalledOrInactive`() = runTest {
        whenever(wooCommerceStore.fetchSitePluginsAndSettings(site))
            .thenReturn(WooResult(WooError(WooErrorType.API_NOT_FOUND, BaseRequest.GenericErrorType.NOT_FOUND)))

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.ForceRefresh))
            .isEqualTo(WooPosWooCorePluginStatus.NotInstalledOrInactive)
    }

    @Test
    fun `given UseCacheAndRefresh, when invoked, then the stored plugins are read and nothing is fetched`() = runTest {
        whenever(wooCommerceStore.getSitePlugins(site))
            .thenReturn(listOf(plugin("woocommerce/woocommerce", "10.1.0", isActive = true)))

        assertThat(sut(WooPosLaunchabilityRefreshPolicy.UseCacheAndRefresh))
            .isEqualTo(WooPosWooCorePluginStatus.Active("10.1.0"))
        verify(wooCommerceStore, never()).fetchSitePluginsAndSettings(site)
    }

    private companion object {
        val KNOWN_FEATURES = EnabledFeatures.Known(listOf("point_of_sale"))
    }
}
