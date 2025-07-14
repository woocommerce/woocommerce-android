package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshBundledProductsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    lateinit var sut: RefreshBundledProducts

    @Before
    fun setUp() {
        sut = RefreshBundledProducts(
            selectedSite,
            productStore,
            coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `check that refresh is only called for the required products`() = testBlocking {
        val remoteProductId = 5L
        val bundledProducts = generateBundledProducts(3)
        val ids = bundledProducts.map { it.bundledProductId }.distinct()
        whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))

        sut.invoke(remoteProductId)

        verify(productStore).fetchProductListSynced(any(), eq(ids))
    }

    private fun generateBundledProducts(n: Int): List<WCBundledProduct> {
        return List(n) {
            WCBundledProduct(
                id = it.toLong(),
                bundledProductId = 20L + it,
                menuOrder = it,
                title = "Bundled product $it",
                stockStatus = "in_stock",
                quantityMin = null,
                quantityMax = null,
                quantityDefault = null,
                isOptional = false,
                attributesDefault = null,
                variationIds = null
            )
        }
    }
}
