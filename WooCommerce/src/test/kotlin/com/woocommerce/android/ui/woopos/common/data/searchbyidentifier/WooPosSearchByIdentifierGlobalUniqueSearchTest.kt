package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
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

class WooPosSearchByIdentifierGlobalUniqueSearchTest {

    private lateinit var sut: WooPosSearchByIdentifierGlobalUniqueSearch
    private val selectedSite: SelectedSite = mock()
    private val productStore: WCProductStore = mock()
    private val site: SiteModel = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierGlobalUniqueSearch(selectedSite, productStore, wooPosLogWrapper)
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given successful search with products, when invoke called, then return success with first product`() = runTest {
        // GIVEN
        val globalUniqueId = "GU123456789"
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
                searchString = eq(null),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.Disabled),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(globalUniqueId)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertTrue(actualResult is WooPosSearchByIdentifierResult.Success)
        val successResult = actualResult as WooPosSearchByIdentifierResult.Success
        assertEquals(expectedProduct.remoteId, successResult.product.remoteId)
        assertEquals(expectedProduct.name, successResult.product.name)
    }

    @Test
    fun `given successful search with no products, when invoke called, then return failure with product not found`() = runTest {
        // GIVEN
        val globalUniqueId = "NONEXISTENT-GU"
        val searchResult = WCProductStore.ProductSearchResult(
            products = emptyList(),
            canLoadMore = false
        )
        val result = WooResult(searchResult)
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(null),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.Disabled),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(globalUniqueId)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound),
            actualResult
        )
    }

    @Test
    fun `given network error, when invoke called, then return failure with network error`() = runTest {
        // GIVEN
        val globalUniqueId = "ERROR-GU"
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(error = mock())
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(null),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.Disabled),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(globalUniqueId)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError),
            actualResult
        )
    }

    @Test
    fun `given null model result, when invoke called, then return failure with unknown error`() = runTest {
        // GIVEN
        val globalUniqueId = "NULL-MODEL-GU"
        val result: WooResult<WCProductStore.ProductSearchResult> = WooResult(model = null)
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = eq(null),
                skuSearchOptions = eq(WCProductStore.SkuSearchOptions.Disabled),
                offset = eq(0),
                pageSize = eq(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE),
                filterOptions = eq(emptyMap()),
                includeTypes = eq(emptyList()),
                orderCurrency = eq(null),
                globalUniqueIdSearchQuery = eq(globalUniqueId)
            )
        ).thenReturn(result)

        // WHEN
        val actualResult = sut(globalUniqueId)

        // THEN
        assertEquals(
            WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError(
                    "Results not found for Global Unique ID: $globalUniqueId"
                )
            ),
            actualResult
        )
    }
}
