package com.woocommerce.android.ui.woopos.home.items.search

import app.cash.turbine.test
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class WooPosSearchProductsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val site = mock<SiteModel>()

    private lateinit var dataSource: WooPosSearchProductsDataSource

    private val sampleProducts = listOf(
        ProductTestUtils.generateProduct(
            productId = 1,
            productName = "Product 1",
            amount = "10.0",
            productType = "simple"
        ),
        ProductTestUtils.generateProduct(
            productId = 2,
            productName = "Product 2",
            amount = "20.0",
            productType = "simple"
        )
    )

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(site)
        dataSource = WooPosSearchProductsDataSource(productStore, selectedSite)
    }

    @Test
    fun `given successful search, when searchProducts called, then both cached and remote results are emitted`() = runTest {
        // Given
        val successResult = WooResult(
            model = WCProductStore.ProductSearchResult(
                products = emptyList(),
                canLoadMore = true
            )
        )
        whenever(
            productStore.searchProducts(
                site = site,
                searchString = "test",
                offset = 0,
                pageSize = 25
            )
        ).thenReturn(successResult)

        // When & Then
        dataSource.searchProducts("test").test {
            val cachedResult = awaitItem() as WooPosSearchProductsDataSource.ProductsResult.Cached
            assertThat(cachedResult.products).isEmpty()

            val remoteResult = awaitItem() as WooPosSearchProductsDataSource.ProductsResult.Remote
            assertThat(remoteResult.productsResult.isSuccess).isTrue()

            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(dataSource.hasMorePages)
    }

    @Test
    fun `given failed search, when searchProducts called, then failure result is emitted`() = runTest {
        // Given
        val wooError = WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN)
        val errorResult = WooResult<WCProductStore.ProductSearchResult>(wooError)

        whenever(
            productStore.searchProducts(
                site = site,
                searchString = "test",
                offset = 0,
                pageSize = 25
            )
        ).thenReturn(errorResult)

        // When & Then
        dataSource.searchProducts("test").test {
            val cachedResult = awaitItem() as WooPosSearchProductsDataSource.ProductsResult.Cached
            assertThat(cachedResult.products).isEmpty()

            val remoteResult = awaitItem() as WooPosSearchProductsDataSource.ProductsResult.Remote
            assertThat(remoteResult.productsResult.isFailure).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given populated cache, when searchProducts called with existing query, then cached results returned first`() = runTest {
        // Given
        val searchQuery = "test"
        val successResult = WooResult(
            model = WCProductStore.ProductSearchResult(
                products = emptyList(),
                canLoadMore = false
            )
        )
        whenever(
            productStore.searchProducts(
                site = site,
                searchString = searchQuery,
                offset = 0,
                pageSize = 25
            )
        ).thenReturn(successResult)

        // Manually inject into cache for testing
        val cacheField = WooPosSearchProductsDataSource::class.java.getDeclaredField("searchResultsCache")
        cacheField.isAccessible = true
        val cache = cacheField.get(dataSource) as MutableMap<String, List<Product>>
        cache[searchQuery.lowercase()] = sampleProducts

        // When
        dataSource.searchProducts(searchQuery).test {
            // Then
            val cachedResult = awaitItem() as WooPosSearchProductsDataSource.ProductsResult.Cached
            assertThat(cachedResult.products).isNotEmpty()
            assertThat(cachedResult.products).isEqualTo(sampleProducts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given product ID, when getProductById called, then returns correct product from cache`() = runTest {
        // Given - Manually inject into cache for testing
        val cacheField = WooPosSearchProductsDataSource::class.java.getDeclaredField("searchResultsCache")
        cacheField.isAccessible = true
        val cache = cacheField.get(dataSource) as MutableMap<String, List<Product>>
        cache["test"] = sampleProducts

        // When
        val product = dataSource.getProductById(sampleProducts[0].remoteId)

        // Then
        assertThat(product).isNotNull()
        assertEquals(sampleProducts[0].remoteId, product?.remoteId)
    }
}
