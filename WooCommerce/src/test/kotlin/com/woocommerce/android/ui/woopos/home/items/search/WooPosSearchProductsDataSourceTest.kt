package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSearchResult

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchProductsDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val productStore: WCProductStore = mock()
    private val wooPosProductsCache: WooPosProductsCache = mock()
    private val searchResultsIndex: WooPosSearchResultsIndex = mock()
    private val selectedSite: SelectedSite = mock()
    private val searchPredicate: WooPosProductSearchPredicate = mock()
    private val siteModel: SiteModel = mock()

    private lateinit var sut: WooPosSearchProductsDataSource

    private val product1 = ProductTestUtils.generateProduct(productId = 1)
    private val product2 = ProductTestUtils.generateProduct(productId = 2)
    private val product3 = ProductTestUtils.generateProduct(productId = 3)
    private val products = listOf(product1, product2, product3)
    private val productsTypesFilterConfig = WooPosProductsTypesFilterConfig()

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(searchPredicate.invoke(any())).thenReturn { _ -> true }

        sut = WooPosSearchProductsDataSource(
            productStore = productStore,
            selectedSite = selectedSite,
            productsCache = wooPosProductsCache,
            searchResultsIndex = searchResultsIndex,
            searchPredicate = searchPredicate,
            productsTypesFilterConfig = productsTypesFilterConfig,
        )
    }

    @Test
    fun `given cached products, when searchLocalProducts called, then should return filtered products`() = runTest {
        // GIVEN
        val query = "test"
        whenever(wooPosProductsCache.getAll()).thenReturn(products)

        // WHEN
        val result = sut.searchLocalProducts(query)

        // THEN
        assertThat(result).isEqualTo(products)
    }

    @Test
    fun `given more products than page size, when searchLocalProducts called, then should limit results to page size`() =
        runTest {
            // GIVEN
            val query = "test"
            val manyProducts = (1..20).map { ProductTestUtils.generateProduct(productId = it.toLong()) }
            whenever(wooPosProductsCache.getAll()).thenReturn(manyProducts)

            // WHEN
            val result = sut.searchLocalProducts(query)

            // THEN
            assertThat(result.size).isEqualTo(15)
        }

    @Test
    fun `given remote search products success, when searchRemoteProducts called, then should return success result`() =
        runTest {
            // GIVEN
            val query = "test"
            whenever(searchResultsIndex.getSearchResults(query)).thenReturn(products)
            whenever(
                productStore.searchProducts(
                    site = anyOrNull(),
                    searchString = anyOrNull(),
                    skuSearchOptions = anyOrNull(),
                    offset = anyOrNull(),
                    pageSize = anyOrNull(),
                    filterOptions = anyOrNull(),
                    includeTypes = anyOrNull(),
                    orderCurrency = anyOrNull(),
                )
            ).thenReturn(WooResult(ProductSearchResult(emptyList(), true)))

            // WHEN
            val result = sut.searchRemoteProducts(query)

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(searchResultsIndex).clearCache()
            assertThat(sut.hasMorePages).isTrue()
        }

    @Test
    fun `given remote search products fail, when searchRemoteProducts called, then should return failure result`() = runTest {
        // GIVEN
        val query = "test"
        val error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            message = "Error fetching products",
            original = BaseRequest.GenericErrorType.UNKNOWN
        )
        whenever(
            productStore.searchProducts(
                site = anyOrNull(),
                searchString = anyOrNull(),
                skuSearchOptions = anyOrNull(),
                offset = anyOrNull(),
                pageSize = anyOrNull(),
                filterOptions = anyOrNull(),
                includeTypes = anyOrNull(),
                orderCurrency = anyOrNull(),
            )
        ).thenReturn(WooResult(error))

        // WHEN
        val result = sut.searchRemoteProducts(query)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given no more pages, when loadMore called, then should return current results without making API call`() =
        runTest {
            // GIVEN
            val query = "test"
            whenever(searchResultsIndex.getSearchResults(query)).thenReturn(products)

            // WHEN
            val result = sut.loadMore(query)

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(products)
            verify(productStore, never()).searchProducts(
                site = anyOrNull(),
                searchString = anyOrNull(),
                skuSearchOptions = anyOrNull(),
                offset = anyOrNull(),
                pageSize = anyOrNull(),
                filterOptions = anyOrNull(),
                includeTypes = anyOrNull(),
                orderCurrency = anyOrNull(),
            )
        }
}
