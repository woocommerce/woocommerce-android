package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class FetchWooCorePluginVersionTest : BaseUnitTest() {

    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private lateinit var sut: FetchWooCorePluginVersion

    @Before
    fun setup() {
        sut = FetchWooCorePluginVersion(
            wooCommerceStore = wooCommerceStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given selected site is null, when invoke is called, then return null`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given fetch result has error, when invoke is called, then return null`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.UNKNOWN
        )
        val fetchResult = WooResult<List<SitePluginModel>>(error)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given fetch result has no error and plugin not found, when invoke is called, then return null`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val plugins: List<SitePluginModel> = emptyList()
        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given fetch result has no error and WooCore plugin found, when invoke is called, then return plugin version`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val version = "1.2.3"
        val wooCorePlugin = mock<SitePluginModel>()
        whenever(wooCorePlugin.name).thenReturn("woocommerce/woocommerce") // CORRECT VALUE
        whenever(wooCorePlugin.version).thenReturn(version)
        whenever(wooCorePlugin.isActive).thenReturn(true)

        val plugins: List<SitePluginModel> = listOf(wooCorePlugin)

        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(version)
    }

    @Test
    fun `given fetch result with WooCore plugin in non-standard directory, when invoke is called, then return plugin version`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val version = "1.2.3"
        val wooCorePlugin = mock<SitePluginModel>()
        whenever(wooCorePlugin.name).thenReturn("woocommerce_in-shoebox/woocommerce")
        whenever(wooCorePlugin.version).thenReturn(version)
        whenever(wooCorePlugin.isActive).thenReturn(true)

        val plugins: List<SitePluginModel> = listOf(wooCorePlugin)

        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(version)
    }

    @Test
    fun `given fetch result with hypothetical single file plugin format, when invoke is called, then return plugin version`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val version = "1.2.3"
        val singleFilePlugin = mock<SitePluginModel>()
        whenever(singleFilePlugin.name).thenReturn("woocommerce") // No directory structure
        whenever(singleFilePlugin.version).thenReturn(version)
        whenever(singleFilePlugin.isActive).thenReturn(true)

        val plugins: List<SitePluginModel> = listOf(singleFilePlugin)

        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(version)
    }

    @Test
    fun `given fetch result with various plugin directory formats, when invoke is called, then return plugin version`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val version = "1.2.3"
        val testCases = listOf(
            "woocommerce/woocommerce",
            "woocommerce-dev/woocommerce",
            "woocommerce-trunk/woocommerce",
            "custom-woo-dir/woocommerce",
            "woocommerce" // Edge case: no directory
        )

        testCases.forEach { pluginName ->
            val wooCorePlugin = mock<SitePluginModel>()
            whenever(wooCorePlugin.name).thenReturn(pluginName)
            whenever(wooCorePlugin.version).thenReturn(version)
            whenever(wooCorePlugin.isActive).thenReturn(true)

            val plugins: List<SitePluginModel> = listOf(wooCorePlugin)
            val fetchResult = WooResult(plugins)
            whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

            // WHEN
            val result = sut()

            // THEN - All formats should work
            assertThat(result).withFailMessage("Failed for plugin name: $pluginName").isEqualTo(version)
        }
    }

    @Test
    fun `given multiple WooCommerce plugins with one active, when invoke is called, then return active plugin version`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val activeVersion = "2.0.0"
        val inactiveVersion = "1.0.0"

        // Inactive plugin (first in list)
        val inactivePlugin = mock<SitePluginModel>()
        whenever(inactivePlugin.name).thenReturn("woocommerce-dev/woocommerce")
        whenever(inactivePlugin.isActive).thenReturn(false)

        // Active plugin (second in list)
        val activePlugin = mock<SitePluginModel>()
        whenever(activePlugin.name).thenReturn("woocommerce/woocommerce")
        whenever(activePlugin.version).thenReturn(activeVersion)
        whenever(activePlugin.isActive).thenReturn(true)

        val plugins: List<SitePluginModel> = listOf(inactivePlugin, activePlugin)
        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(activeVersion)
    }

    @Test
    fun `given multiple WooCommerce plugins all inactive, when invoke is called, then return null`() = runTest {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)

        val firstVersion = "1.0.0"
        val secondVersion = "2.0.0"

        // First inactive plugin
        val firstPlugin = mock<SitePluginModel>()
        whenever(firstPlugin.name).thenReturn("woocommerce-dev/woocommerce")
        whenever(firstPlugin.isActive).thenReturn(false)

        // Second inactive plugin
        val secondPlugin = mock<SitePluginModel>()
        whenever(secondPlugin.name).thenReturn("woocommerce/woocommerce")
        whenever(secondPlugin.isActive).thenReturn(false)

        val plugins: List<SitePluginModel> = listOf(firstPlugin, secondPlugin)
        val fetchResult = WooResult(plugins)
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(null)
    }
}
