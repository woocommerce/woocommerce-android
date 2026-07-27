package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_ID
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRestClientTest {
    lateinit var sut: ProductRestClient

    private val productId = 5L
    private val site = SiteModel()
    private val wooNetwork: WooNetwork = mock {
        on {
            executePostGsonRequest(
                any(), any(), eq(BatchProductApiResponse::class.java), any()
            )
        } doReturn WPAPIResponse.Success(null, emptyList())
    }
    private val wpComNetwork: WPComNetwork = mock()

    @Before fun setUp() {
        sut = ProductRestClient(mock(), wooNetwork, wpComNetwork, initCoroutineEngine(), mock(), mock())
    }

    @Test
    fun `when duplicating a product, then exact endpoint and empty body are used and duplicated ID is parsed`() =
        runTest {
            // GIVEN
            val response = Gson().fromJson(
                """
                    {
                      "id": 123,
                      "name": "Coffee (Copy)",
                      "slug": "coffee-copy",
                      "date_created": {"date": "2026-07-14 12:00:00", "timezone_type": 3, "timezone": "UTC"},
                      "category_ids": [12],
                      "image_id": 8,
                      "gallery_image_ids": [9],
                      "meta_data": [{"id": 9, "key": "_subscription_price", "value": "10"}],
                      "downloads": []
                    }
                """.trimIndent(),
                DuplicateProductApiResponse::class.java
            )
            whenever(
                wooNetwork.executePostGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = eq(DuplicateProductApiResponse::class.java),
                    body = any()
                )
            ).thenReturn(WPAPIResponse.Success(response, emptyList()))

            // WHEN
            val result = sut.duplicateProduct(site, productId)

            // THEN
            verify(wooNetwork).executePostGsonRequest(
                site = eq(site),
                path = eq(WOOCOMMERCE.products.id(productId).duplicate.pathV3),
                clazz = eq(DuplicateProductApiResponse::class.java),
                body = eq(emptyMap())
            )
            assertThat(result.result?.id).isEqualTo(123L)
        }

    @Test
    fun `given duplicate route is missing, when duplicating a product, then rest no route error is preserved`() =
        runTest {
            // GIVEN
            val networkError = WPAPINetworkError(
                BaseNetworkError(NOT_FOUND, "No route"),
                errorCode = "rest_no_route"
            )
            whenever(
                wooNetwork.executePostGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = eq(DuplicateProductApiResponse::class.java),
                    body = any()
                )
            ).thenReturn(WPAPIResponse.Error(networkError))

            // WHEN
            val result = sut.duplicateProduct(site, productId)

            // THEN
            assertThat(result.error?.type).isEqualTo(API_NOT_FOUND)
            assertThat(result.error?.apiErrorCode).isEqualTo("rest_no_route")
        }

    @Test
    fun `given source product is invalid, when duplicating a product, then invalid ID error is not treated as missing route`() =
        runTest {
            // GIVEN
            val networkError = WPAPINetworkError(
                BaseNetworkError(NOT_FOUND, "Invalid product ID"),
                errorCode = "woocommerce_rest_product_invalid_id"
            )
            whenever(
                wooNetwork.executePostGsonRequest(
                    site = any(),
                    path = any(),
                    clazz = eq(DuplicateProductApiResponse::class.java),
                    body = any()
                )
            ).thenReturn(WPAPIResponse.Error(networkError))

            // WHEN
            val result = sut.duplicateProduct(site, productId)

            // THEN
            assertThat(result.error?.type).isEqualTo(INVALID_ID)
            assertThat(result.error?.apiErrorCode).isEqualTo("woocommerce_rest_product_invalid_id")
        }

    @Test
    fun `send only updated parameters with id if products differ`() = runTest {
        // given
        val product = WCProductModel().copy(
            remoteId = RemoteId(productId),
            status = "unchanged status"
        )
        val bodyCaptor = argumentCaptor<Map<String, Any>> { }

        // when
        sut.batchUpdateProducts(
            site,
            mapOf(
                product.withRegularPrice("20") to product.withRegularPrice("10")
            )
        )

        // then
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.products.batch.pathV3),
            clazz = eq(BatchProductApiResponse::class.java),
            body = bodyCaptor.capture()
        )
        assertThat(bodyCaptor.allValues).hasSize(1)
        assertThat(bodyCaptor.firstValue).isEqualTo(
            mapOf(
                ("update" to listOf(
                    mapOf<String, Any>(
                        ("id" to productId), ("regular_price" to "10")
                    )
                ))
            )
        )
    }

    @Test
    fun `do not send any properties if entities do not differ`() = runTest {
        // given
        val productA = WCProductModel().copy(
            remoteId = RemoteId(2),
            status = "unchanged status"
        )
        val productB = WCProductModel().copy(
            remoteId = RemoteId(3),
            status = "other, unchanged status"
        )
        val bodyCaptor = argumentCaptor<Map<String, Any>> { }

        // when
        sut.batchUpdateProducts(
            site,
            mapOf(
                productA to productA,
                productB to productB,
            )
        )

        // then
        verify(wooNetwork, never()).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.products.batch.pathV3),
            clazz = eq(BatchProductApiResponse::class.java),
            body = bodyCaptor.capture()
        )
    }

    @Test
    fun `when direct product batch patch is requested, then requested patch is sent for every id`() = runTest {
        // given
        val bodyCaptor = argumentCaptor<Map<String, Any>> { }
        whenever(
            wooNetwork.executePostGsonRequest(
                any(), any(), eq(BatchProductUpdateApiResponse::class.java), any()
            )
        ) doReturn WPAPIResponse.Success(BatchProductUpdateApiResponse(emptyList()), emptyList())

        // when
        sut.batchUpdateProductsPatch(
            site = site,
            updateRequests = mapOf(
                42L to WCProductStore.UpdateProductRequest(
                    name = "Updated",
                    regularPrice = "12.50",
                    salePrice = "10.00",
                    stockQuantity = 5,
                    status = "publish",
                ),
                43L to WCProductStore.UpdateProductRequest(
                    name = "Updated",
                    regularPrice = "12.50",
                    salePrice = "10.00",
                    stockQuantity = 5,
                    status = "publish",
                ),
            )
        )

        // then
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.products.batch.pathV3),
            clazz = eq(BatchProductUpdateApiResponse::class.java),
            body = bodyCaptor.capture()
        )
        assertThat(bodyCaptor.firstValue).isEqualTo(
            mapOf(
                "update" to listOf(
                    mapOf(
                        "id" to 42L,
                        "name" to "Updated",
                        "regular_price" to "12.50",
                        "sale_price" to "10.00",
                        "stock_quantity" to 5,
                        "manage_stock" to true,
                        "status" to "publish",
                    ),
                    mapOf(
                        "id" to 43L,
                        "name" to "Updated",
                        "regular_price" to "12.50",
                        "sale_price" to "10.00",
                        "stock_quantity" to 5,
                        "manage_stock" to true,
                        "status" to "publish",
                    )
                )
            )
        )
    }

    @Test
    fun `when fetch products called with exact sku search, then correct params is used for network call`() {
        runTest {
            whenever(wooNetwork.executeGetGsonRequest(any(), any(), eq(Array<ProductApiResponse>::class.java), any(), any(), any(), any(), any(), any())).thenReturn(WPAPIResponse.Success(null, emptyList()))
            sut.fetchProducts(
                site = site,
                searchQuery = "test query",
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
            )
            val argumentCaptor = argumentCaptor<MutableMap<String, String>>()
            verify(wooNetwork).executeGetGsonRequest(
                any(),
                any(),
                clazz = eq(Array<ProductApiResponse>::class.java),
                params = argumentCaptor.capture(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )

            assertThat(argumentCaptor.firstValue.getOrDefault("sku", null)).isEqualTo(
                "test query"
            )
            assertThat(argumentCaptor.firstValue.getOrDefault("search_sku", null)).isNull()
        }
    }

    @Test
    fun `when fetch products called with partial sku search, then correct params is used for network call`() {
        runTest {
            whenever(wooNetwork.executeGetGsonRequest(any(), any(), eq(Array<ProductApiResponse>::class.java), any(), any(), any(), any(), any(), any())).thenReturn(WPAPIResponse.Success(null, emptyList()))
            sut.fetchProducts(
                site = site,
                searchQuery = "test query",
                skuSearchOptions = WCProductStore.SkuSearchOptions.PartialMatch
            )
            val argumentCaptor = argumentCaptor<MutableMap<String, String>>()
            verify(wooNetwork).executeGetGsonRequest(
                any(),
                any(),
                clazz = eq(Array<ProductApiResponse>::class.java),
                params = argumentCaptor.capture(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )

            assertThat(argumentCaptor.firstValue.getOrDefault("search_sku", null)).isEqualTo(
                "test query"
            )
        }
    }

    @Test
    fun `when fetch products called with the global unique id, then correct params is used for network call`() {
        runTest {
            whenever(wooNetwork.executeGetGsonRequest(any(), any(), eq(Array<ProductApiResponse>::class.java), any(), any(), any(), any(), any(), any())).thenReturn(WPAPIResponse.Success(null, emptyList()))
            val globalUniqueIdSearchQuery = "test global unique id"
            sut.fetchProducts(
                site = site,
                globalUniqueIdSearchQuery = globalUniqueIdSearchQuery
            )
            val argumentCaptor = argumentCaptor<MutableMap<String, String>>()
            verify(wooNetwork).executeGetGsonRequest(
                any(),
                any(),
                clazz = eq(Array<ProductApiResponse>::class.java),
                params = argumentCaptor.capture(),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                requestTimeout = any(),
                retries = any()
            )

            assertThat(argumentCaptor.firstValue.getOrDefault("global_unique_id", null)).isEqualTo(
                globalUniqueIdSearchQuery
            )
        }
    }

    private fun WCProductModel.withRegularPrice(newRegularPrice: String): WCProductModel =
        copy(
            remoteId = RemoteId(this@withRegularPrice.remoteProductId),
            regularPrice = newRegularPrice
        )
}
