package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
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

    private fun makeProduct(id: Long = 1L, name: String = "Test Product"): WCProductModel =
        WCProductModel(remoteId = RemoteId(id), name = name)

    private fun makeStoredProduct(
        id: Long = 1L,
        name: String = "Test Product",
        type: String = "simple",
        regularPrice: String = "10.00",
        salePrice: String = "",
        manageStock: Boolean = false,
        stockQuantity: Double = 0.0,
        status: String = "publish",
    ) = WCProductModel(
        remoteId = RemoteId(id),
        name = name,
        type = type,
        regularPrice = regularPrice,
        salePrice = salePrice,
        manageStock = manageStock,
        stockQuantity = stockQuantity,
        status = status,
    )

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
    fun `given page and perPage over max, when fetchProducts is called, then offset uses clamped page size`() = runTest {
        whenever(
            productStore.fetchProducts(
                site = any(),
                offset = eq(50),
                pageSize = eq(50),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any(),
                posProductsOnly = any(),
            )
        ).thenReturn(WooResult(emptyList()))

        dataSource.fetchProducts(page = 2, perPage = 999)

        verify(productStore).fetchProducts(
            site = any(),
            offset = eq(50),
            pageSize = eq(50),
            sortType = any(),
            filterOptions = any(),
            includeTypes = any(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given 20 products returned, when fetchProducts is called, then canLoadMore is true`() = runTest {
        val products = (1..20).map { WCProductModel() }
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
        val products = (1..5).map { WCProductModel() }
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

    // --- updateProduct ---

    @Test
    fun `given simple product, when updateProduct is called, then provided fields are applied to batch payload`() =
        runTest {
            val product = makeStoredProduct(
                id = 10L,
                name = "Original",
                regularPrice = "9.00",
                salePrice = "5.00",
                manageStock = false,
                stockQuantity = 1.0,
                status = "publish",
            )
            whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(product)
            whenever(productStore.batchUpdateProducts(any())).thenReturn(
                WooResult(
                    listOf(
                        product.copy(
                            name = "Updated",
                            regularPrice = "12.50",
                            stockQuantity = 4.0,
                            manageStock = true,
                            status = "draft",
                        )
                    )
                )
            )

            val result = dataSource.updateProduct(
                productId = 10L,
                update = AIProductsDataSource.ProductUpdate(
                    name = "Updated",
                    regularPrice = "12.50",
                    stockQuantity = 4,
                    status = "draft",
                )
            )

            assertThat(result.isSuccess).isTrue
            val payloadCaptor = argumentCaptor<WCProductStore.BatchUpdateProductsPayload>()
            verify(productStore).batchUpdateProducts(payloadCaptor.capture())
            val updatedProduct = payloadCaptor.firstValue.updatedProducts.single()
            assertThat(payloadCaptor.firstValue.site).isEqualTo(site)
            assertThat(updatedProduct.name).isEqualTo("Updated")
            assertThat(updatedProduct.regularPrice).isEqualTo("12.50")
            assertThat(updatedProduct.salePrice).isEqualTo("5.00")
            assertThat(updatedProduct.stockQuantity).isEqualTo(4.0)
            assertThat(updatedProduct.manageStock).isTrue
            assertThat(updatedProduct.status).isEqualTo("draft")
        }

    @Test
    fun `given product is not cached, when updateProduct is called, then product is fetched before update`() = runTest {
        val product = makeStoredProduct(id = 10L)
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(null).thenReturn(product)
        whenever(productStore.fetchSingleProduct(any())).thenReturn(
            WCProductStore.OnProductChanged(remoteProductId = 10L)
        )
        whenever(productStore.batchUpdateProducts(any())).thenReturn(WooResult(listOf(product.copy(name = "Updated"))))

        val result = dataSource.updateProduct(
            productId = 10L,
            update = AIProductsDataSource.ProductUpdate(name = "Updated")
        )

        assertThat(result.isSuccess).isTrue
        verify(productStore).fetchSingleProduct(any())
        verify(productStore).batchUpdateProducts(any())
    }

    @Test
    fun `given variable product, when updateProduct is called, then batch update is not called`() = runTest {
        val product = makeStoredProduct(id = 10L, type = "variable")
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(product)

        val result = dataSource.updateProduct(
            productId = 10L,
            update = AIProductsDataSource.ProductUpdate(name = "Updated")
        )

        assertThat(result.exceptionOrNull())
            .isInstanceOf(AIProductsDataSource.UnsupportedProductTypeException::class.java)
        verify(productStore, never()).batchUpdateProducts(any())
    }

    @Test
    fun `given batch update fails, when updateProduct is called, then failure result is returned`() = runTest {
        val product = makeStoredProduct(id = 10L)
        whenever(productStore.getProductByRemoteId(site, 10L)).thenReturn(product)
        whenever(productStore.batchUpdateProducts(any())).thenReturn(
            WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom"))
        )

        val result = dataSource.updateProduct(
            productId = 10L,
            update = AIProductsDataSource.ProductUpdate(name = "Updated")
        )

        assertThat(result.isFailure).isTrue
    }

    @Test
    fun `given product patch, when bulkUpdateProducts is called, then one direct batch update is sent`() = runTest {
        whenever(productStore.batchUpdateProducts(eq(site), any())).thenReturn(
            WooResult(WCProductStore.UpdateProductsResult(updatedProducts = listOf(10L, 11L)))
        )

        val result = dataSource.bulkUpdateProducts(
            productIds = listOf(10L, 11L),
            update = AIProductsDataSource.ProductUpdate(
                name = "Updated",
                regularPrice = "12.50",
                salePrice = "9.99",
                stockQuantity = 5,
                status = "publish",
            )
        )

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrThrow().updatedIds).containsExactly(10L, 11L)
        val requestsCaptor = argumentCaptor<Map<Long, WCProductStore.UpdateProductRequest>>()
        verify(productStore).batchUpdateProducts(eq(site), requestsCaptor.capture())
        assertThat(requestsCaptor.firstValue.keys).containsExactly(10L, 11L)
        requestsCaptor.firstValue.values.forEach { request ->
            assertThat(request.name).isEqualTo("Updated")
            assertThat(request.regularPrice).isEqualTo("12.50")
            assertThat(request.salePrice).isEqualTo("9.99")
            assertThat(request.stockQuantity).isEqualTo(5)
            assertThat(request.status).isEqualTo("publish")
        }
        verify(productStore, never()).fetchSingleProduct(any())
    }

    @Test
    fun `given store returns partial product failures, when bulkUpdateProducts is called, then failures are exposed`() =
        runTest {
            val failedProduct = WCProductStore.UpdateProductsResult.FailedProduct(
                id = 11L,
                errorCode = "woocommerce_rest_product_invalid_id",
                errorMessage = "Invalid ID.",
                errorStatus = 400,
            )
            whenever(productStore.batchUpdateProducts(eq(site), any())).thenReturn(
                WooResult(
                    WCProductStore.UpdateProductsResult(
                        updatedProducts = listOf(10L),
                        failedProducts = listOf(failedProduct),
                    )
                )
            )

            val result = dataSource.bulkUpdateProducts(
                productIds = listOf(10L, 11L),
                update = AIProductsDataSource.ProductUpdate(name = "Updated")
            )

            assertThat(result.isSuccess).isTrue
            assertThat(result.getOrThrow().updatedIds).containsExactly(10L)
            assertThat(result.getOrThrow().failedProducts).containsExactly(failedProduct)
        }

    // --- getProducts (cache-first batch) ---

    @Test
    fun `given all requested products are cached, when getProducts is called, then no fetch is made`() = runTest {
        val products = listOf(makeProduct(id = 10L), makeProduct(id = 11L))
        whenever(productStore.getProductsByRemoteIds(site, listOf(10L, 11L))).thenReturn(products)

        val result = dataSource.getProducts(productIds = listOf(10L, 11L)).getOrThrow()

        assertThat(result.items).containsExactlyElementsOf(products)
        assertThat(result.cacheHitCount).isEqualTo(2)
        assertThat(result.cacheMissCount).isEqualTo(0)
        assertThat(result.fetchAttempted).isFalse
        assertThat(result.fetchFailed).isFalse
        verify(productStore, never()).fetchProducts(
            site = any(),
            offset = any(),
            pageSize = any(),
            sortType = any(),
            includedProductIds = any(),
            excludedProductIds = any(),
            filterOptions = any(),
            includeTypes = any(),
            forceRefresh = any(),
            orderCurrency = anyOrNull(),
            posProductsOnly = any(),
        )
    }

    @Test
    fun `given some products are cached and fetch fails, when getProducts is called, then cached products are returned`() =
        runTest {
            val cached = makeProduct(id = 10L)
            whenever(productStore.getProductsByRemoteIds(site, listOf(10L, 11L))).thenReturn(listOf(cached))
            whenever(
                productStore.fetchProducts(
                    site = any(),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    includedProductIds = any(),
                    excludedProductIds = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    forceRefresh = any(),
                    orderCurrency = anyOrNull(),
                    posProductsOnly = any(),
                )
            ).thenReturn(WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom")))

            val result = dataSource.getProducts(productIds = listOf(10L, 11L)).getOrThrow()

            assertThat(result.items).containsExactly(cached)
            assertThat(result.cacheHitCount).isEqualTo(1)
            assertThat(result.cacheMissCount).isEqualTo(1)
            assertThat(result.fetchAttempted).isTrue
            assertThat(result.fetchFailed).isTrue
        }

    @Test
    fun `given some products are cached and fetch succeeds, when getProducts is called, then all products are returned`() =
        runTest {
            val cached = makeProduct(id = 10L)
            val fetched = makeProduct(id = 11L)
            whenever(productStore.getProductsByRemoteIds(site, listOf(10L, 11L)))
                .thenReturn(listOf(cached))
                .thenReturn(listOf(cached, fetched))
            whenever(
                productStore.fetchProducts(
                    site = any(),
                    offset = eq(0),
                    pageSize = eq(1),
                    sortType = any(),
                    includedProductIds = eq(listOf(11L)),
                    excludedProductIds = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    forceRefresh = eq(false),
                    orderCurrency = anyOrNull(),
                    posProductsOnly = any(),
                )
            ).thenReturn(WooResult(true))

            val result = dataSource.getProducts(productIds = listOf(10L, 11L)).getOrThrow()

            assertThat(result.items).containsExactly(cached, fetched)
            assertThat(result.cacheHitCount).isEqualTo(1)
            assertThat(result.cacheMissCount).isEqualTo(1)
            assertThat(result.fetchAttempted).isTrue
            assertThat(result.fetchFailed).isFalse
        }

    @Test
    fun `given batch update fails, when bulkUpdateProducts is called, then failure result is returned`() = runTest {
        whenever(productStore.batchUpdateProducts(eq(site), any())).thenReturn(
            WooResult(WooError(WooErrorType.API_ERROR, GenericErrorType.SERVER_ERROR, "boom"))
        )

        val result = dataSource.bulkUpdateProducts(
            productIds = listOf(10L),
            update = AIProductsDataSource.ProductUpdate(name = "Updated")
        )

        assertThat(result.isFailure).isTrue
    }
}
