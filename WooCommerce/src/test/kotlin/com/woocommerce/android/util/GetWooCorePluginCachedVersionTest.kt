package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetWooCorePluginCachedVersionTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val getWooCorePluginCachedVersion = GetWooCorePluginCachedVersion(
        wooCommerceStore = wooCommerceStore,
        selectedSite = selectedSite
    )

    @Test
    fun `given selected site is null, when invoke is called, then return null`() {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val result = getWooCorePluginCachedVersion()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given selected site is not null and store returns null, when invoke is called, then return null`() {
        // GIVEN
        val siteModel = mock<SiteModel>()
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        whenever(
            wooCommerceStore.getSitePlugin(
                siteModel,
                WooCommerceStore.WooPlugin.WOO_CORE
            )
        ).thenReturn(null)

        // WHEN
        val result = getWooCorePluginCachedVersion()

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given selected site is not null and store returns not null, when invoke is called, then return store response`() {
        // GIVEN
        val siteModel = mock<SiteModel>()
        val version = "1.0.0"
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        val sitePluginModel = mock<SitePluginModel> {
            on { this.version }.thenReturn(version)
        }
        whenever(
            wooCommerceStore.getSitePlugin(
                siteModel,
                WooCommerceStore.WooPlugin.WOO_CORE
            )
        ).thenReturn(sitePluginModel)

        // WHEN
        val result = getWooCorePluginCachedVersion()

        // THEN
        assertThat(result).isEqualTo(version)
    }
}
