package com.woocommerce.android.ui.products.details

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class DetermineProductPasswordApiTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val wooCommerceStore: WooCommerceStore = mock()

    val sut: DetermineProductPasswordApi = DetermineProductPasswordApi(
        selectedSite,
        wooCommerceStore
    )

    @Test
    fun `when site has WooCommerce higher than 8_1, then use CORE API`() = testBlocking {
        whenever(wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(SitePluginModel().apply { version = "8.1.0" })

        val result = sut()

        assert(result == ProductPasswordApi.CORE)
    }

    @Test
    fun `given a Jetpack connection, when site has WooCommerce lower than 8_1, then use WPCOM API`() = testBlocking {
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.Jetpack)
        whenever(wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(SitePluginModel().apply { version = "8.0.0" })

        val result = sut()

        assert(result == ProductPasswordApi.WPCOM)
    }

    @Test
    fun `given a JetpackCP connection, when site has WooCommerce lower than 8_1, then use WPCOM API`() = testBlocking {
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.JetpackConnectionPackage)
        whenever(wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE))
            .thenReturn(SitePluginModel().apply { version = "8.0.0" })

        val result = sut()

        assert(result == ProductPasswordApi.WPCOM)
    }

    @Test
    fun `given an ApplicationPasswords connection, when site has WooCommerce lower than 8_1, then API is not supported`() =
        testBlocking {
            whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
            whenever(wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE))
                .thenReturn(SitePluginModel().apply { version = "8.0.0" })

            val result = sut()

            assert(result == ProductPasswordApi.UNSUPPORTED)
        }
}
