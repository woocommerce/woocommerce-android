package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.selector.ProductListHandler
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

class WooPosProductsDataSourceTest {
    private val handler: ProductListHandler = mock()
    private val productStore: WCProductStore = mock()
    private val site: SelectedSite = mock()

    @Before
    fun setup() {
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(site.getOrNull()).thenReturn(SiteModel())
    }

    @Test
    fun `when force refreshing, then should wipe products table`() = runTest {
        createSut().loadSimpleProducts(forceRefreshProducts = true)
        verify(productStore).deleteProductsForSite(anyOrNull())
    }

    private fun createSut() = WooPosProductsDataSource(handler, productStore, site)
}
