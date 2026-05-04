package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
        val orderOne = order(id = 1L, number = "1001")
        val orderTwo = order(id = 2L, number = "1002")
        val product = product(id = 3L, name = "Socks")
        whenever(ordersDataSource.getOrders(listOf(1L, 2L))).thenReturn(
            Result.success(orderLookup(orderTwo, orderOne))
        )
        whenever(productsDataSource.getProducts(listOf(3L))).thenReturn(Result.success(productLookup(product)))

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.Product, "3"),
                ref(ShowCardFamily.Order, "2"),
            )
        )

        verify(ordersDataSource).getOrders(listOf(1L, 2L))
        verify(productsDataSource).getProducts(listOf(3L))
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
    fun `given missing order, when resolved, then missing ref is not found`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(99L))).thenReturn(Result.success(orderLookup()))

        val result = resolver.resolve(listOf(ref(ShowCardFamily.Order, "99")))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.NotFound)
    }

    @Test
    fun `given order fetch fails and product succeeds, when resolved, then product still resolves`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(1L))).thenReturn(Result.failure(IllegalStateException("boom")))
        whenever(productsDataSource.getProducts(listOf(2L))).thenReturn(
            Result.success(productLookup(product(id = 2L)))
        )

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.Product, "2"),
            )
        )

        val orderResult = result[0] as ShowCardsResolution.Missing
        val productResult = result[1] as ShowCardsResolution.Resolved
        assertThat(orderResult.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
        assertThat(productResult.summary.getValue("id").jsonPrimitive.content).isEqualTo("2")
    }

    @Test
    fun `given resolved entities, when resolved, then summaries and cards use compact app-owned fields`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(1L))).thenReturn(Result.success(orderLookup(order(id = 1L))))
        whenever(productsDataSource.getProducts(listOf(2L))).thenReturn(
            Result.success(productLookup(product(id = 2L)))
        )

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.Product, "2"),
            )
        ).filterIsInstance<ShowCardsResolution.Resolved>()

        assertThat(
            result[0].summary.keys
        ).containsExactly(
            "id",
            "number",
            "status",
            "total",
            "currency",
            "date_created",
        )
        assertThat(result[0].card.family).isEqualTo("order")
        assertThat(result[0].card.id).isEqualTo("1")
        assertThat(result[0].card.title).isEqualTo("#1")
        val orderDetails = result[0].card.details as ShowCardDetails.Order
        assertThat(orderDetails.status).isEqualTo("processing")
        assertThat(orderDetails.total).isEqualTo("12.34")
        assertThat(orderDetails.currency).isEqualTo("USD")
        assertThat(orderDetails.dateCreated).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(result[1].summary.keys).containsExactly("id", "name", "sku", "price", "stock_status")
        assertThat(result[1].card.family).isEqualTo("product")
        assertThat(result[1].card.id).isEqualTo("2")
        assertThat(result[1].card.title).isEqualTo("Socks")
        val productDetails = result[1].card.details as ShowCardDetails.Product
        assertThat(productDetails.sku).isEqualTo("woo-socks")
        assertThat(productDetails.price).isEqualTo("9.99")
        assertThat(productDetails.stockStatus).isEqualTo("instock")
        assertThat(productDetails.status).isEqualTo("publish")
    }

    @Test
    fun `given cached order and failed fetch for missing order, when resolved, then cached order is returned`() =
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
    fun `given fetch succeeds but product id is absent, when resolved, then missing product is not found`() = runTest {
        whenever(productsDataSource.getProducts(listOf(10L, 11L))).thenReturn(
            Result.success(
                CachedLookupResult(
                    items = listOf(product(id = 10L)),
                    cacheHitCount = 0,
                    cacheMissCount = 2,
                    fetchAttempted = true,
                    fetchFailed = false,
                )
            )
        )

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Product, "10"),
                ref(ShowCardFamily.Product, "11"),
            )
        )

        assertThat(result[0]).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        val missing = result[1] as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.NotFound)
    }

    private fun ref(family: ShowCardFamily, id: String) = ValidatedRef(index = 0, family = family, id = id)

    private fun orderLookup(vararg orders: OrderEntity): CachedLookupResult<OrderEntity> =
        CachedLookupResult(
            items = orders.toList(),
            cacheHitCount = 0,
            cacheMissCount = orders.size,
            fetchAttempted = true,
            fetchFailed = false,
        )

    private fun productLookup(vararg products: WCProductModel): CachedLookupResult<WCProductModel> =
        CachedLookupResult(
            items = products.toList(),
            cacheHitCount = 0,
            cacheMissCount = products.size,
            fetchAttempted = true,
            fetchFailed = false,
        )

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
