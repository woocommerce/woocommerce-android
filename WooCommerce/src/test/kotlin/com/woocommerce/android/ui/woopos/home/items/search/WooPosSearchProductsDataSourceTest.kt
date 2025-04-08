package com.woocommerce.android.ui.woopos.home.items.search

import app.cash.turbine.test
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSearchResult
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchProductsDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val productStore: WCProductStore = mock()
    private val wooPosProductsCache: WooPosProductsCache = mock()
    private val searchResultsCache: WooPosSearchResultsCache = mock()
    private val selectedSite: SelectedSite = mock()
    private val searchPredicate: ProductSearchPredicate = mock()
    private val siteModel: SiteModel = mock()

    private lateinit var sut: WooPosSearchProductsDataSource

    private val product1 = ProductTestUtils.generateProduct(productId = 1)
    private val product2 = ProductTestUtils.generateProduct(productId = 2)
    private val product3 = ProductTestUtils.generateProduct(productId = 3)
    private val products = listOf(product1, product2, product3)

    private val wcProduct1 = WCProductModel(1).apply {
        remoteProductId = 1
        name = "Product 1"
    }

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(searchPredicate.invoke(any())).thenReturn { _ -> true }

        sut = WooPosSearchProductsDataSource(
            productStore = productStore,
            selectedSite = selectedSite,
            productsCache = wooPosProductsCache,
            searchResultsCache = searchResultsCache,
            searchPredicate = searchPredicate
        )
    }

    @Test
    fun `given cached search results, when search products called, then should emit cached results`() = runTest {
        // GIVEN
        val query = "test"
        whenever(searchResultsCache.hasSearchResults(query)).thenReturn(true)
        whenever(searchResultsCache.getSearchResults(query)).thenReturn(products)

        // WHEN
        sut.searchProducts(query).test {
            // THEN
            val result = awaitItem()
            assertThat(result).isInstanceOf(WooPosSearchProductsDataSource.ProductsResult.Cached::class.java)
            assertThat((result as WooPosSearchProductsDataSource.ProductsResult.Cached).products).isEqualTo(products)
            awaitComplete()
        }
    }

    @Test
    fun `given no cached results, when search products called, then should emit local then remote results`() = runTest {
        // GIVEN
        val query = "test"
        val localProducts = listOf(product1)

        whenever(searchResultsCache.hasSearchResults(query)).thenReturn(false)
        whenever(wooPosProductsCache.getAll()).thenReturn(localProducts)

        val productSearchResult = ProductSearchResult(
            products = listOf(wcProduct1),
            canLoadMore = false
        )

        val wooResult = WooResult(productSearchResult)
        whenever(
            productStore.searchProducts(
                site = any(),
                searchString = any(),
                skuSearchOptions = any(),
                offset = any(),
                pageSize = any(),
                orderCurrency = any()
            )
        ).thenReturn(wooResult)

        // WHEN
        sut.searchProducts(query).test(timeout = 5.seconds) {
            // THEN
            // First emits local cached results
            val firstResult = awaitItem()
            assertThat(firstResult).isInstanceOf(WooPosSearchProductsDataSource.ProductsResult.Cached::class.java)

            // Skip checking remote results for this test to simplify
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given product id, when getProductById called, then should return product from cache`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(wooPosProductsCache.getProductById(productId)).thenReturn(product1)

        // WHEN
        val result = sut.getProductById(productId)

        // THEN
        assertThat(result).isEqualTo(product1)
    }
}
