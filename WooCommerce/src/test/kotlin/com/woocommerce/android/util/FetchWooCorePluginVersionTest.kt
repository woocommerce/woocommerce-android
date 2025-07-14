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

        val fetchResult = mock<WooResult<List<SitePluginModel>>> {
            on { isError }.thenReturn(true)
        }
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
        val fetchResult = mock<WooResult<List<SitePluginModel>>> {
            on { isError }.thenReturn(false)
            on { model }.thenReturn(plugins)
        }
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

        val plugins: List<SitePluginModel> = listOf(wooCorePlugin)

        val fetchResult = mock<WooResult<List<SitePluginModel>>> {
            on { isError }.thenReturn(false)
            on { model }.thenReturn(plugins)
        }
        whenever(wooCommerceStore.fetchSitePlugins(siteModel)).thenReturn(fetchResult)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isEqualTo(version)
    }
}
