package com.woocommerce.android.ui.orders.creation.customerlistnew

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
class CustomerListIsAdvancedSearchSupportedTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSiteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(selectedSiteModel)
    }

    private val action = CustomerListIsAdvancedSearchSupported(
        wooCommerceStore = wooCommerceStore,
        dispatchers = coroutinesTestRule.testDispatchers,
        selectedSite = selectedSite,
    )

    @Test
    fun `given version lower than 8, when action invoked, then false returned`() = testBlocking {
        // GIVEN
        val version = "7.9.9"
        val sitePluginModel = mock<SitePluginModel> {
            on { this.version }.thenReturn(version)
        }
        whenever(wooCommerceStore.getSitePlugin(selectedSiteModel, WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(sitePluginModel)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version null, when action invoked, then false returned`() = testBlocking {
        // GIVEN
        val version = null
        val sitePluginModel = mock<SitePluginModel> {
            on { this.version }.thenReturn(version)
        }
        whenever(wooCommerceStore.getSitePlugin(selectedSiteModel, WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(sitePluginModel)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version 8, when action invoked, then true returned`() = testBlocking {
        // GIVEN
        val version = "8.0.0"
        val sitePluginModel = mock<SitePluginModel> {
            on { this.version }.thenReturn(version)
        }
        whenever(wooCommerceStore.getSitePlugin(selectedSiteModel, WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(sitePluginModel)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given version more than 8, when action invoked, then true returned`() = testBlocking {
        // GIVEN
        val version = "8.0.1"
        val sitePluginModel = mock<SitePluginModel> {
            on { this.version }.thenReturn(version)
        }
        whenever(wooCommerceStore.getSitePlugin(selectedSiteModel, WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(sitePluginModel)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isTrue()
    }
}
