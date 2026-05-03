package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

class ShowCardsResolverTest {
    private val ordersDataSource: AIOrdersDataSource = mock()
    private val productsDataSource: AIProductsDataSource = mock()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
    private val resolver = DefaultShowCardsResolver(
        ordersDataSource = ordersDataSource,
        productsDataSource = productsDataSource,
        json = json,
    )

    @Test
    fun `given mixed refs, when resolved, then output preserves input order`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(1L, 2L))).thenReturn(
            Result.success(
                CachedLookupResult(
                    items = listOf(order(id = 2L), order(id = 1L)),
                    cacheHitCount = 2,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        )
        whenever(productsDataSource.getProducts(listOf(3L))).thenReturn(
            Result.success(
                CachedLookupResult(
                    items = listOf(product(id = 3L)),
                    cacheHitCount = 1,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        )

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.Product, "3"),
                ref(ShowCardFamily.Order, "2"),
            )
        )

        assertThat(result.map { it.ref.family.serializedName to it.ref.id }).containsExactly(
            "order" to "1",
            "product" to "3",
            "order" to "2",
        )
        assertThat(result).allSatisfy { resolution ->
            assertThat(resolution).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        }
    }

    @Test
    fun `given missing order after successful fetch, when resolved, then missing ref is not found`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(99L))).thenReturn(
            Result.success(
                CachedLookupResult(
                    items = emptyList(),
                    cacheHitCount = 0,
                    cacheMissCount = 1,
                    fetchAttempted = true,
                    fetchFailed = false,
                )
            )
        )

        val result = resolver.resolve(listOf(ref(ShowCardFamily.Order, "99")))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.NotFound)
    }

    @Test
    fun `given cached order and fetch failure for missing, when resolved, then cached resolves and missing is fetch failed`() =
        runTest {
            whenever(ordersDataSource.getOrders(listOf(1L, 2L))).thenReturn(
                Result.success(
                    CachedLookupResult(
                        items = listOf(order(id = 1L)),
                        cacheHitCount = 1,
                        cacheMissCount = 1,
                        fetchAttempted = true,
                        fetchFailed = true,
                    )
                )
            )

            val result = resolver.resolve(
                listOf(
                    ref(ShowCardFamily.Order, "1"),
                    ref(ShowCardFamily.Order, "2"),
                )
            )

            assertThat(result[0]).isInstanceOf(ShowCardsResolution.Resolved::class.java)
            val missing = result[1] as ShowCardsResolution.Missing
            assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
        }

    @Test
    fun `given order data source fails entirely and product succeeds, when resolved, then product still resolves`() =
        runTest {
            whenever(ordersDataSource.getOrders(listOf(1L))).thenReturn(
                Result.failure(IllegalStateException("boom"))
            )
            whenever(productsDataSource.getProducts(listOf(2L))).thenReturn(
                Result.success(
                    CachedLookupResult(
                        items = listOf(product(id = 2L)),
                        cacheHitCount = 1,
                        cacheMissCount = 0,
                        fetchAttempted = false,
                        fetchFailed = false,
                    )
                )
            )

            val result = resolver.resolve(
                listOf(
                    ref(ShowCardFamily.Order, "1"),
                    ref(ShowCardFamily.Product, "2"),
                )
            )

            val orderResult = result[0] as ShowCardsResolution.Missing
            assertThat(orderResult.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
            assertThat(result[1]).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        }

    @Test
    fun `given resolved entities, when resolved, then summaries contain compact fields and cards have correct family`() =
        runTest {
            whenever(ordersDataSource.getOrders(listOf(1L))).thenReturn(
                Result.success(
                    CachedLookupResult(
                        items = listOf(order(id = 1L)),
                        cacheHitCount = 1,
                        cacheMissCount = 0,
                        fetchAttempted = false,
                        fetchFailed = false,
                    )
                )
            )
            whenever(productsDataSource.getProducts(listOf(2L))).thenReturn(
                Result.success(
                    CachedLookupResult(
                        items = listOf(product(id = 2L)),
                        cacheHitCount = 1,
                        cacheMissCount = 0,
                        fetchAttempted = false,
                        fetchFailed = false,
                    )
                )
            )

            val result = resolver.resolve(
                listOf(
                    ref(ShowCardFamily.Order, "1"),
                    ref(ShowCardFamily.Product, "2"),
                )
            ).filterIsInstance<ShowCardsResolution.Resolved>()

            val expectedOrderKeys = listOf("id", "number", "status", "total", "currency", "date_created")
            assertThat(result[0].summary.keys).containsExactlyElementsOf(expectedOrderKeys)
            assertThat(result[0].card.family).isEqualTo("order")
            assertThat(result[1].summary.keys).containsExactly("id", "name", "sku", "price", "stock_status")
            assertThat(result[1].card.family).isEqualTo("product")
        }

    private fun ref(family: ShowCardFamily, id: String) = ValidatedRef(index = 0, family = family, id = id)

    private fun order(
        id: Long,
        number: String = id.toString(),
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = id,
        number = number,
        status = "processing",
        total = "12.34",
        currency = "USD",
        dateCreated = "2026-05-01T10:00:00Z",
    )

    private fun product(
        id: Long,
        name: String = "Socks",
    ) = WCProductModel(
        remoteId = RemoteId(id),
        name = name,
        sku = "woo-socks",
        price = "9.99",
        stockStatus = "instock",
        status = "publish",
    )
}
