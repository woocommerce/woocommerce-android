package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

class WooPosSearchByIdentifierSkuSearchTest {

    private lateinit var sut: WooPosSearchByIdentifierSkuSearch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val site: SiteModel = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierSkuSearch(selectedSite, productStore)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given successful search with products, when invoke called, then return success with first product`() = runTest {
        // GIVEN
        val sku = "TEST-SKU-123"
        val wcProduct = ProductTestUtils.generateWCProductModel()
        val expectedProduct = wcProduct.toAppModel()
        val searchResult = WCProductStore.ProductSearchResult(
            products = listOf(wcProduct),
            canLoadMore = false
        )
        val result = WooResult(searchResult)
        whenever(
            productStore.searchProducts(
                site = eq(site),
                searchString = eq(sku),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(null)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(sku)

        // THEN
        assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
        val successResult = actualResult as WooPosSearchByIdentifierResult.Success
        assertEquals(expectedProduct.remoteId, successResult.product.remoteId)
        assertEquals(expectedProduct.name, successResult.product.name)
    }

    @Test
    fun `given successful search with no products, when invoke called, then return failure with product not found`() = runTest {
        // GIVEN
        val sku = "NONEXISTENT-SKU"
        val searchResult = WCProductStore.ProductSearchResult(
            products = emptyList(),
            canLoadMore = false
        )
        val result = WooResult(searchResult)
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(sku),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(null)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(sku)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given network error, when invoke called, then return failure with network error`() = runTest {
        // GIVEN
        val sku = "ERROR-SKU"
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = mock())
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(sku),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(null)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(sku)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError),
            actualResult
        )
    }

    @Test
    fun `given null model result, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val sku = "NULL-MODEL-SKU"
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(model = null)
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(sku),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.ExactSearch),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(null)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(sku)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Results not found for SKU: $sku")
            ),
            actualResult
        )
    }
}
