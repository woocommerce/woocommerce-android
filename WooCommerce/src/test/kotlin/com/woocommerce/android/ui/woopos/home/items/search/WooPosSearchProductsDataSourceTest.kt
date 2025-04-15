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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSearchResult

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSearchProductsDataSourceTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    private val productStore: WCProductStore = mock()
    private val wooPosProductsCache: WooPosProductsCache = mock()
    private val searchResults: WooPosSearchResultsIndex = mock()
    private val selectedSite: SelectedSite = mock()
    private val searchPredicate: ProductSearchPredicate = mock()
    private val siteModel: SiteModel = mock()

    private lateinit var sut: WooPosSearchProductsDataSource

    private val product1 = ProductTestUtils.generateProduct(productId = 1)
    private val product2 = ProductTestUtils.generateProduct(productId = 2)
    private val product3 = ProductTestUtils.generateProduct(productId = 3)
    private val products = listOf(product1, product2, product3)

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(searchPredicate.invoke(any())).thenReturn { _ -> true }

        sut = WooPosSearchProductsDataSource(
            productStore = productStore,
            selectedSite = selectedSite,
            productsCache = wooPosProductsCache,
            searchResultsIndex = searchResults,
            searchPredicate = searchPredicate
        )
    }

    @Test
    fun `given cached search results, when search products called, then should emit cached results`() = runTest {
        // GIVEN
        val query = "test"
        whenever(searchResults.hasSearchResults(query)).thenReturn(true)
        whenever(searchResults.getSearchResults(query)).thenReturn(emptyList())
        whenever(wooPosProductsCache.getAll()).thenReturn(products)
        whenever(
            productStore.searchProducts(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        ).thenReturn(WooResult(ProductSearchResult(emptyList(), false)))

        // WHEN
        sut.searchProducts(query).test {
            // THEN
            val result = awaitItem()
            assertThat(result).isInstanceOf(WooPosSearchProductsDataSource.ProductsResult.Cached::class.java)
            assertThat((result as WooPosSearchProductsDataSource.ProductsResult.Cached).products).isEqualTo(products)
            verify(searchResults).storeSearchResults(query, emptyList())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given more products than page size, when search products called, then should limit local results to page size`() =
        runTest {
            // GIVEN
            val query = "test"
            val manyProducts = (1..20).map { ProductTestUtils.generateProduct(productId = it.toLong()) }
            whenever(wooPosProductsCache.getAll()).thenReturn(manyProducts)
            whenever(
                productStore.searchProducts(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            )
                .thenReturn(WooResult(ProductSearchResult(emptyList(), false)))

            // WHEN
            sut.searchProducts(query).test {
                // THEN
                val result = awaitItem()
                assertThat(result).isInstanceOf(WooPosSearchProductsDataSource.ProductsResult.Cached::class.java)
                assertThat((result as WooPosSearchProductsDataSource.ProductsResult.Cached).products.size).isEqualTo(15)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
