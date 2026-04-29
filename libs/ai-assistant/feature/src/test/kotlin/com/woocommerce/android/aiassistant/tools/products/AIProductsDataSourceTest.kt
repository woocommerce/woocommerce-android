package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

@ExperimentalCoroutinesApi
class AIProductsDataSourceTest {

    private val site: SiteModel = SiteModel().apply { id = 42 }
    private val selectedSite: SelectedSite = mock { on { get() }.thenReturn(site) }
    private val productStore: WCProductStore = mock()

    private val dataSource = AIProductsDataSource(
        selectedSite = selectedSite,
        productStore = productStore,
    )

    private fun makeProduct(id: Long = 1L, name: String = "Test Product"): WCProductModel = mock {
        on { remoteProductId }.thenReturn(id)
        on { this.name }.thenReturn(name)
    }

    private suspend fun stubFetchProducts(result: WooResult<List<WCProductModel>>) {
        whenever(
            productStore.fetchProducts(
                site = any(),
                offset = any(),
                pageSize = any(),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any(),
                posProductsOnly = any(),
            )
        ).thenReturn(result)
    }

    // --- fetchProducts ---

    @Test
    fun `given no search query, when fetchProducts is called, then fetchProducts store method is used`() = runTest {
        stubFetchProducts(WooResult(emptyList()))

        val result = dataSource.fetchProducts(search = null)

        assertThat(result.isSuccess).isTrue
    }

    @Test
    fun `given search query, when fetchProducts is called, then searchProductsByNameAndSku is used`() = runTest {
        whenever(
            productStore.searchProductsByNameAndSku(
                site = any(),
                searchNameOrSkuQuery = eq("shirt"),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any(),
                searchFields = anyOrNull(),
                posProductsOnly = any(),
            )
        ).thenReturn(WooResult(WCProductStore.ProductSearchResult(emptyList(), false)))

        val result = dataSource.fetchProducts(search = "shirt")

        assertThat(result.isSuccess).isTrue
        verify(productStore, never()).fetchProducts(
            site = any(),
            offset = any(),
            pageSize = any(),
            sortType = any(),
            filterOptions = any(),
            includeTypes = any(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given blank search query, when fetchProducts is called, then search is normalised to null`() = runTest {
        stubFetchProducts(WooResult(emptyList()))

        val result = dataSource.fetchProducts(search = "   ")

        assertThat(result.isSuccess).isTrue
        verify(productStore, never()).searchProductsByNameAndSku(
            site = any(),
            searchNameOrSkuQuery = any(),
            offset = any(),
            pageSize = any(),
            filterOptions = any(),
            includeTypes = any(),
            searchFields = anyOrNull(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given page = 2 and perPage = 10, when fetchProducts is called, then offset = 10 is used`() = runTest {
        whenever(
            productStore.fetchProducts(
                site = any(),
                offset = eq(10),
                pageSize = eq(10),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any(),
                posProductsOnly = any(),
            )
        ).thenReturn(WooResult(emptyList()))

        dataSource.fetchProducts(page = 2, perPage = 10)

        verify(productStore).fetchProducts(
            site = any(),
            offset = eq(10),
            pageSize = eq(10),
            sortType = any(),
            filterOptions = any(),
            includeTypes = any(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given perPage over max, when fetchProducts is called, then it is clamped to 50`() = runTest {
        whenever(
            productStore.fetchProducts(
                site = any(),
                offset = any(),
                pageSize = eq(50),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any(),
                posProductsOnly = any(),
            )
        ).thenReturn(WooResult(emptyList()))

        dataSource.fetchProducts(perPage = 999)

        verify(productStore).fetchProducts(
            site = any(),
            offset = any(),
            pageSize = eq(50),
            sortType = any(),
            filterOptions = any(),
            includeTypes = any(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given 20 products returned, when fetchProducts is called, then canLoadMore is true`() = runTest {
        val products = (1..20).map { mock<WCProductModel>() }
        whenever(
            productStore.fetchProducts(
                site = any(),
                offset = any(),
                pageSize = eq(20),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any(),
                posProductsOnly = any(),
            )
        ).thenReturn(WooResult(products))

        val result = dataSource.fetchProducts(perPage = 20)

        assertThat(result.getOrThrow().canLoadMore).isTrue
    }

    @Test
    fun `given fewer than 20 products returned, when fetchProducts is called, then canLoadMore is false`() = runTest {
        val products = (1..5).map { mock<WCProductModel>() }
        stubFetchProducts(WooResult(products))

        val result = dataSource.fetchProducts()

        assertThat(result.getOrThrow().canLoadMore).isFalse
    }

    @Test
    fun `given store returns an error, when fetchProducts is called, then failure result is returned`() = runTest {
        stubFetchProducts(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom")))

        val result = dataSource.fetchProducts()

        assertThat(result.isFailure).isTrue
    }

    // --- getProduct ---

    @Test
    fun `given product is cached, when getProduct is called, then no network call is made`() = runTest {
        val product = makeProduct(id = 10L)
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(product)

        val result = dataSource.getProduct(productId = 10L)

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrThrow()).isEqualTo(product)
        verify(productStore, never()).fetchSingleProduct(any())
    }

    @Test
    fun `given product not cached, when getProduct is called, then fetchSingleProduct is called`() = runTest {
        val product = makeProduct(id = 10L)
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(null).thenReturn(product)
        val event = WCProductStore.OnProductChanged(remoteProductId = 10L)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(event)

        val result = dataSource.getProduct(productId = 10L)

        assertThat(result.isSuccess).isTrue
        verify(productStore).fetchSingleProduct(any())
    }

    @Test
    fun `given network fetch fails, when getProduct is called, then failure result is returned`() = runTest {
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(null)
        val errorEvent = WCProductStore.OnProductChanged().also {
            it.error = WCProductStore.ProductError(message = "not found")
        }
        whenever(productStore.fetchSingleProduct(any())).thenReturn(errorEvent)

        val result = dataSource.getProduct(productId = 10L)

        assertThat(result.isFailure).isTrue
    }
}
