package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class GetAnalyticPluginsCardActiveTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val isGoogleForWooEnabled: IsGoogleForWooEnabled = mock()

    private val defaultPluginsResult = listOf(
        SitePluginModel().apply {
            name = WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName
            setIsActive(true)
        }
    )
    private val defaultSite = SiteModel()

    private lateinit var sut: GetAnalyticPluginsCardActive

    @Before
    fun setup() {
        sut = GetAnalyticPluginsCardActive(
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            isGoogleForWooEnabled = isGoogleForWooEnabled
        )
    }

    @Test
    fun `when bundles plugin is active then bundle card in active plugin cards`() = testBlocking {
        whenever(isGoogleForWooEnabled.invoke()).thenReturn(false)
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        whenever(wooCommerceStore.getSitePlugins(any(), any())).thenReturn(defaultPluginsResult)

        val result = sut.invoke()
        assertThat(result).isNotEmpty()
        assertThat(result).contains(AnalyticsCards.Bundles)
    }

    @Test
    fun `when bundles plugin is NOT active then bundle card is not in active plugin cards`() = testBlocking {
        whenever(isGoogleForWooEnabled.invoke()).thenReturn(false)
        val pluginResult = listOf(
            SitePluginModel().apply {
                name = WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES.pluginName
                setIsActive(false)
            }
        )
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        whenever(wooCommerceStore.getSitePlugins(any(), any())).thenReturn(pluginResult)

        val result = sut.invoke()
        assertThat(result).doesNotContain(AnalyticsCards.Bundles)
    }

    @Test
    fun `when selected site fails the the list of active plugins is empty`() = testBlocking {
        whenever(isGoogleForWooEnabled.invoke()).thenReturn(false)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val result = sut.invoke()
        assertThat(result).isEmpty()
    }

    @Test
    fun `when google ads plugin is active then google ads card in active plugin cards`() = testBlocking {
        val pluginResult = listOf(
            SitePluginModel().apply {
                name = WooCommerceStore.WooPlugin.GOOGLE_ADS.pluginName
                setIsActive(true)
            }
        )
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        whenever(wooCommerceStore.getSitePlugins(any(), any())).thenReturn(pluginResult)
        whenever(isGoogleForWooEnabled.invoke()).thenReturn(true)

        val result = sut.invoke()
        assertThat(result).contains(AnalyticsCards.GoogleAds)
    }

    @Test
    fun `when google ads plugin is NOT active then google ads card is not in active plugin cards`() = testBlocking {
        val pluginResult = listOf(
            SitePluginModel().apply {
                name = WooCommerceStore.WooPlugin.GOOGLE_ADS.pluginName
                setIsActive(true)
            }
        )
        whenever(selectedSite.getOrNull()).thenReturn(defaultSite)
        whenever(wooCommerceStore.getSitePlugins(any(), any())).thenReturn(pluginResult)
        whenever(isGoogleForWooEnabled.invoke()).thenReturn(false)

        val result = sut.invoke()
        assertThat(result).doesNotContain(AnalyticsCards.GoogleAds)
    }
}
