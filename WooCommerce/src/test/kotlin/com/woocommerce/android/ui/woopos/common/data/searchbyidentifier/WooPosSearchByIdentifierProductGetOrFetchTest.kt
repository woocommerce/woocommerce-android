package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore

class WooPosSearchByIdentifierProductGetOrFetchTest {

    private lateinit var sut: WooPosSearchByIdentifierProductGetOrFetch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val productsCache: WooPosProductsCache = mock()
    private val site: SiteModel = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierProductGetOrFetch(selectedSite, productStore, productsCache)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given successful fetch with product, when invoke called, then return success and cache product`() = runTest {
        // GIVEN
        val productId = 123L
        val wcProduct = ProductTestUtils.generateWCProductModel()
        val product = wcProduct.toAppModel()
        val result: WCProductStore.OnProductChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(
            productStore.fetchSingleProduct(any())
        ).thenReturn(result)

        whenever(productStore.getProduct(site, productId)).thenReturn(wcProduct)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
        val successResult = actualResult as WooPosSearchByIdentifierResult.Success
        assertEquals(product.remoteId, successResult.product.remoteId)
        assertEquals(product.name, successResult.product.name)
    }

    @Test
    fun `given network error during fetch, when invoke called, then return failure with network error`() = runTest {
        // GIVEN
        val productId = 123L
        val result: WCProductStore.OnProductChanged = mock {
            on { isError }.thenReturn(true)
        }

        whenever(
            productStore.fetchSingleProduct(any())
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError),
            actualResult
        )
    }

    @Test
    fun `given successful fetch but product not found in store, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val productId = 123L
        val result: WCProductStore.OnProductChanged = mock {
            on { isError }.thenReturn(false)
        }

        whenever(
            productStore.fetchSingleProduct(any())
        ).thenReturn(result)

        whenever(productStore.getProduct(site, productId)).thenReturn(null)

        // WHEN
        val actualResult = sut(productId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Product not found for ID: $productId")
            ),
            actualResult
        )
    }
}
