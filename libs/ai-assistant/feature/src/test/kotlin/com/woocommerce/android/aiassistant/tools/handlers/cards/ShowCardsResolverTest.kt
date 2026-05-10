package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.analytics.AIAnalyticsDataSource
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsStats
import com.woocommerce.android.aiassistant.tools.customers.AICustomersDataSource
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

class ShowCardsResolverTest {
    private val ordersDataSource: AIOrdersDataSource = mock()
    private val productsDataSource: AIProductsDataSource = mock()
    private val analyticsDataSource: AIAnalyticsDataSource = mock()
    private val customersDataSource: AICustomersDataSource = mock()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val resolver = DefaultShowCardsResolver(
        ordersDataSource = ordersDataSource,
        productsDataSource = productsDataSource,
        analyticsDataSource = analyticsDataSource,
        customersDataSource = customersDataSource,
        json = json,
    )

    @Test
    fun `given mixed refs, when resolved, then output preserves input order`() = runTest {
        val orderOne = order(id = 1L, number = "1001")
        val orderTwo = order(id = 2L, number = "1002")
        val product = product(id = 3L, name = "Socks")
        val customer = customer(id = 4L, firstName = "Ada", lastName = "Lovelace", email = "ada@example.com")
        val stats = analyticsStats()
        whenever(ordersDataSource.getOrders(listOf(1L, 2L))).thenReturn(
            Result.success(orderLookup(orderTwo, orderOne))
        )
        whenever(productsDataSource.getProducts(listOf(3L))).thenReturn(Result.success(productLookup(product)))
        givenCustomersFetch(ids = listOf(4L), customer)
        whenever(
            analyticsDataSource.fetchRevenueStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        ).thenReturn(Result.success(stats))

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.AnalyticsStats, ANALYTICS_STATS_ID),
                ref(ShowCardFamily.Product, "3"),
                ref(ShowCardFamily.Customer, "4"),
                ref(ShowCardFamily.Order, "2"),
            )
        )

        verify(ordersDataSource).getOrders(listOf(1L, 2L))
        verify(productsDataSource).getProducts(listOf(3L))
        verifyCustomersFetch(ids = listOf(4L))
        assertThat(result.map { it.ref.family.serializedName to it.ref.id }).containsExactly(
            "order" to "1",
            "analytics_stats" to ANALYTICS_STATS_ID,
            "product" to "3",
            "customer" to "4",
            "order" to "2",
        )
        assertThat(result).allSatisfy { resolution ->
            assertThat(resolution).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        }
    }

    @Test
    fun `given analytics stats ref, when resolved, then resolver refetches stats from parsed id`() = runTest {
        whenever(
            analyticsDataSource.fetchRevenueStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        ).thenReturn(Result.success(analyticsStats()))

        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_STATS_ID)))

        verify(analyticsDataSource).fetchRevenueStats(
            after = "2026-05-01T00:00:00",
            before = "2026-05-07T23:59:59",
            interval = AnalyticsInterval.DAY,
            currency = "USD",
        )
        verifyNoInteractions(ordersDataSource, productsDataSource)
        val resolved = result.single() as ShowCardsResolution.Resolved
        assertThat(resolved.summary.getValue("id").jsonPrimitive.content).isEqualTo(ANALYTICS_STATS_ID)
        assertThat(resolved.summary.getValue("after").jsonPrimitive.content).isEqualTo("2026-05-01")
        assertThat(resolved.summary.getValue("before").jsonPrimitive.content).isEqualTo("2026-05-07")
        assertThat(resolved.summary.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
        assertThat(resolved.summary.getValue("kind").jsonPrimitive.content).isEqualTo("revenue")
        assertThat(resolved.summary.getValue("totals").jsonObject.getValue("total_sales").jsonPrimitive.content)
            .isEqualTo("170.35")
        assertThat(resolved.summary.getValue("interval_subtotals").jsonArray).hasSize(2)
        assertThat(resolved.card.family).isEqualTo("analytics_stats")
        assertThat(resolved.card.id).isEqualTo(ANALYTICS_STATS_ID)
        assertThat(resolved.card.title).isEqualTo("Analytics")
        val details = resolved.card.details as ShowCardDetails.AnalyticsStats
        assertThat(details.kind).isEqualTo("revenue")
        assertThat(details.totals.getValue("net_revenue").jsonPrimitive.content).isEqualTo("120.15")
        assertThat(details.intervalSubtotals).hasSize(2)
    }

    @Test
    fun `given revenue and orders analytics stats refs, when resolved, then resolver fetches each stats kind`() = runTest {
        whenever(
            analyticsDataSource.fetchRevenueStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        ).thenReturn(Result.success(analyticsStats()))
        whenever(
            analyticsDataSource.fetchOrdersStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
        ).thenReturn(Result.success(orderAnalyticsStats()))
        whenever(analyticsDataSource.getSelectedSiteCurrencyCode()).thenReturn("USD")

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.AnalyticsStats, ANALYTICS_STATS_ID),
                ref(ShowCardFamily.AnalyticsStats, ANALYTICS_ORDERS_STATS_ID),
            )
        )

        verify(analyticsDataSource).fetchRevenueStats(
            after = "2026-05-01T00:00:00",
            before = "2026-05-07T23:59:59",
            interval = AnalyticsInterval.DAY,
            currency = "USD",
        )
        verify(analyticsDataSource).fetchOrdersStats(
            after = "2026-05-01T00:00:00",
            before = "2026-05-07T23:59:59",
            interval = AnalyticsInterval.DAY,
        )
        assertThat(result).allSatisfy { resolution ->
            assertThat(resolution).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        }
        assertThat(result.map { it.ref.id }).containsExactly(ANALYTICS_STATS_ID, ANALYTICS_ORDERS_STATS_ID)
        assertThat(
            result.filterIsInstance<ShowCardsResolution.Resolved>()
                .map { it.summary.getValue("kind").jsonPrimitive.content }
        ).containsExactly("revenue", "orders")
    }

    @Test
    fun `given analytics stats ref with currency none, when resolved, then site currency is used for display`() = runTest {
        whenever(
            analyticsDataSource.fetchRevenueStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = null,
            )
        ).thenReturn(Result.success(analyticsStats()))
        whenever(analyticsDataSource.getSelectedSiteCurrencyCode()).thenReturn("USD")

        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_STATS_ID_NO_CURRENCY)))

        verify(analyticsDataSource).fetchRevenueStats(
            after = "2026-05-01T00:00:00",
            before = "2026-05-07T23:59:59",
            interval = AnalyticsInterval.DAY,
            currency = null,
        )
        val resolved = result.single() as ShowCardsResolution.Resolved
        assertThat(resolved.summary.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
        val details = resolved.card.details as ShowCardDetails.AnalyticsStats
        assertThat(details.currency).isEqualTo("USD")
    }

    @Test
    fun `given orders analytics stats ref, when resolved, then resolver refetches orders stats only`() = runTest {
        whenever(
            analyticsDataSource.fetchOrdersStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
        ).thenReturn(Result.success(orderAnalyticsStats()))
        whenever(analyticsDataSource.getSelectedSiteCurrencyCode()).thenReturn("USD")

        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_ORDERS_STATS_ID)))

        verify(analyticsDataSource).fetchOrdersStats(
            after = "2026-05-01T00:00:00",
            before = "2026-05-07T23:59:59",
            interval = AnalyticsInterval.DAY,
        )
        verify(analyticsDataSource, never()).fetchRevenueStats(any(), any(), any(), any())
        val resolved = result.single() as ShowCardsResolution.Resolved
        assertThat(resolved.summary.getValue("kind").jsonPrimitive.content).isEqualTo("orders")
        assertThat(resolved.summary.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
    }

    @Test
    fun `given orders analytics stats ref without currency, when resolved, then resolver refetches orders stats`() =
        runTest {
            whenever(
                analyticsDataSource.fetchOrdersStats(
                    after = "2026-05-01T00:00:00",
                    before = "2026-05-07T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.success(orderAnalyticsStats()))
            whenever(analyticsDataSource.getSelectedSiteCurrencyCode()).thenReturn("USD")

            val result = resolver.resolve(
                listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_ORDERS_STATS_ID_NO_CURRENCY))
            )

            verify(analyticsDataSource).fetchOrdersStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
            verify(analyticsDataSource, never()).fetchRevenueStats(any(), any(), any(), any())
            val resolved = result.single() as ShowCardsResolution.Resolved
            assertThat(resolved.summary.getValue("kind").jsonPrimitive.content).isEqualTo("orders")
            assertThat(resolved.summary.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
        }

    @Test
    fun `given analytics stats refetch fails, when resolved, then ref is fetch failed`() = runTest {
        whenever(
            analyticsDataSource.fetchRevenueStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        ).thenReturn(Result.failure(IllegalStateException("network")))

        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_STATS_ID)))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
    }

    @Test
    fun `given orders analytics stats refetch fails, when resolved, then ref is fetch failed`() = runTest {
        whenever(
            analyticsDataSource.fetchOrdersStats(
                after = "2026-05-01T00:00:00",
                before = "2026-05-07T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
        ).thenReturn(Result.failure(IllegalStateException("network")))

        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, ANALYTICS_ORDERS_STATS_ID)))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
    }

    @Test
    fun `given malformed analytics stats id reaches resolver, when resolved, then ref is invalid id`() = runTest {
        val result = resolver.resolve(listOf(ref(ShowCardFamily.AnalyticsStats, "bad-id")))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.InvalidId)
        verifyNoInteractions(analyticsDataSource)
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
            "customer_name",
            "line_items_count",
        )
        assertThat(result[0].summary.getValue("customer_name").jsonPrimitive.content).isEqualTo("Jane Doe")
        assertThat(result[0].card.family).isEqualTo("order")
        assertThat(result[0].card.id).isEqualTo("1")
        assertThat(result[0].card.title).isEqualTo("#1")
        val orderDetails = result[0].card.details as ShowCardDetails.Order
        assertThat(orderDetails.status).isEqualTo("processing")
        assertThat(orderDetails.total).isEqualTo("12.34")
        assertThat(orderDetails.currency).isEqualTo("USD")
        assertThat(orderDetails.dateCreated).isEqualTo("2026-05-01T10:00:00Z")
        assertThat(orderDetails.customerName).isEqualTo("Jane Doe")
        assertThat(result[1].summary.keys).containsExactly(
            "id",
            "name",
            "sku",
            "price",
            "type",
            "stock_status",
            "manage_stock",
            "on_sale",
            "stock_quantity",
        )
        assertThat(result[1].card.family).isEqualTo("product")
        assertThat(result[1].card.id).isEqualTo("2")
        assertThat(result[1].card.title).isEqualTo("Socks")
        val productDetails = result[1].card.details as ShowCardDetails.Product
        assertThat(productDetails.sku).isEqualTo("woo-socks")
        assertThat(productDetails.price).isEqualTo("9.99")
        assertThat(productDetails.stockStatus).isEqualTo("instock")
        assertThat(productDetails.status).isEqualTo("publish")
        assertThat(productDetails.imageUrl).isEqualTo(PRODUCT_IMAGE_URL)
    }

    @Test
    fun `given resolved entities, when resolved, then summaries include widened compact fields`() = runTest {
        whenever(ordersDataSource.getOrders(listOf(1L))).thenReturn(
            Result.success(
                orderLookup(
                    order(
                        id = 1L,
                        customerId = 55L,
                        paymentMethodTitle = "Credit Card",
                        lineItems = ORDER_LINE_ITEMS_JSON,
                    )
                )
            )
        )
        whenever(productsDataSource.getProducts(listOf(2L))).thenReturn(
            Result.success(
                productLookup(
                    product(
                        id = 2L,
                        type = "simple",
                        manageStock = true,
                        onSale = false,
                        stockQuantity = 12.0,
                    )
                )
            )
        )

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Order, "1"),
                ref(ShowCardFamily.Product, "2"),
            )
        ).filterIsInstance<ShowCardsResolution.Resolved>()

        assertThat(result[0].summary.keys).contains(
            "payment_method_title",
            "customer_id",
            "line_items_count",
            "line_items",
        )
        assertThat(result[0].summary.getValue("payment_method_title").jsonPrimitive.content)
            .isEqualTo("Credit Card")
        assertThat(result[0].summary.getValue("customer_id").jsonPrimitive.content).isEqualTo("55")
        assertThat(result[0].summary.getValue("line_items_count").jsonPrimitive.content).isEqualTo("2")
        assertThat(result[0].summary.getValue("line_items").jsonArray).hasSize(2)
        assertThat(result[1].summary.keys).contains("manage_stock", "on_sale", "type", "stock_quantity")
        assertThat(result[1].summary.getValue("type").jsonPrimitive.content).isEqualTo("simple")
        assertThat(result[1].summary.getValue("manage_stock").jsonPrimitive.boolean).isTrue
        assertThat(result[1].summary.getValue("on_sale").jsonPrimitive.boolean).isFalse
        assertThat(result[1].summary.getValue("stock_quantity").jsonPrimitive.double).isEqualTo(12.0)
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

    @Test
    fun `given customer refs, when resolved, then customers are fetched by include and compact cards are returned`() =
        runTest {
            val customerOne = customer(id = 123L, firstName = "Ada", lastName = "Lovelace", email = "ada@example.com")
            val customerTwo = customer(id = 456L, firstName = "Grace", lastName = "Hopper", email = "grace@example.com")
            givenCustomersFetch(ids = listOf(123L, 456L), customerTwo, customerOne)

            val result = resolver.resolve(
                listOf(
                    ref(ShowCardFamily.Customer, "123"),
                    ref(ShowCardFamily.Customer, "456"),
                )
            )

            verifyCustomersFetch(ids = listOf(123L, 456L))
            val resolved = result.filterIsInstance<ShowCardsResolution.Resolved>()
            assertThat(resolved.map { it.ref.id }).containsExactly("123", "456")
            assertThat(resolved[0].summary.keys).containsExactly("id", "name", "email")
            assertThat(resolved[0].summary.getValue("id").jsonPrimitive.content).isEqualTo("123")
            assertThat(resolved[0].summary.getValue("name").jsonPrimitive.content).isEqualTo("Ada Lovelace")
            assertThat(resolved[0].summary.getValue("email").jsonPrimitive.content).isEqualTo("ada@example.com")
            assertThat(resolved[0].summary).doesNotContainKeys("phone", "address")
            assertThat(resolved[0].card.family).isEqualTo("customer")
            assertThat(resolved[0].card.id).isEqualTo("123")
            assertThat(resolved[0].card.title).isEqualTo("Ada Lovelace")
            val details = resolved[0].card.details as ShowCardDetails.Customer
            assertThat(details.email).isEqualTo("ada@example.com")
        }

    @Test
    fun `given customer fetch succeeds but id is absent, when resolved, then missing customer is not found`() = runTest {
        givenCustomersFetch(ids = listOf(123L, 456L), customer(id = 123L))

        val result = resolver.resolve(
            listOf(
                ref(ShowCardFamily.Customer, "123"),
                ref(ShowCardFamily.Customer, "456"),
            )
        )

        assertThat(result[0]).isInstanceOf(ShowCardsResolution.Resolved::class.java)
        val missing = result[1] as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.NotFound)
    }

    @Test
    fun `given customer fetch fails, when resolved, then customer refs are fetch failed`() = runTest {
        givenCustomersFetchFailure(ids = listOf(123L))

        val result = resolver.resolve(listOf(ref(ShowCardFamily.Customer, "123")))

        val missing = result.single() as ShowCardsResolution.Missing
        assertThat(missing.reason).isEqualTo(ShowCardsRejectionReason.FetchFailed)
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

    private suspend fun givenCustomersFetch(
        ids: List<Long>,
        vararg customers: WCCustomerModel,
    ) {
        whenever(
            customersDataSource.fetchCustomers(
                search = null,
                email = null,
                include = ids,
                orderby = "registered_date",
                order = "desc",
                page = null,
                perPage = ids.size,
            )
        ).thenReturn(Result.success(customers.toList()))
    }

    private suspend fun givenCustomersFetchFailure(ids: List<Long>) {
        whenever(
            customersDataSource.fetchCustomers(
                search = null,
                email = null,
                include = ids,
                orderby = "registered_date",
                order = "desc",
                page = null,
                perPage = ids.size,
            )
        ).thenReturn(Result.failure(IllegalStateException("network")))
    }

    private suspend fun verifyCustomersFetch(ids: List<Long>) {
        verify(customersDataSource).fetchCustomers(
            search = null,
            email = null,
            include = ids,
            orderby = "registered_date",
            order = "desc",
            page = null,
            perPage = ids.size,
        )
    }

    private fun order(
        id: Long,
        number: String = id.toString(),
        customerId: Long = 0,
        paymentMethodTitle: String = "",
        lineItems: String = "",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = id,
        number = number,
        status = "processing",
        total = "12.34",
        currency = "USD",
        dateCreated = "2026-05-01T10:00:00Z",
        customerId = customerId,
        paymentMethodTitle = paymentMethodTitle,
        billingFirstName = "Jane",
        billingLastName = "Doe",
        lineItems = lineItems,
    )

    private fun product(
        id: Long,
        name: String = "Socks",
        type: String = "simple",
        manageStock: Boolean = false,
        onSale: Boolean = false,
        stockQuantity: Double = 0.0,
    ) = WCProductModel(
        remoteId = RemoteId(id),
        name = name,
        type = type,
        sku = "woo-socks",
        price = "9.99",
        stockStatus = "instock",
        manageStock = manageStock,
        onSale = onSale,
        stockQuantity = stockQuantity,
        status = "publish",
        images = """[{"id":7,"src":"$PRODUCT_IMAGE_URL"}]""",
    )

    private fun customer(
        id: Long,
        firstName: String = "Ada",
        lastName: String = "Lovelace",
        email: String = "ada@example.com",
    ) = WCCustomerModel(
        localSiteId = LocalId(1),
        remoteCustomerId = RemoteId(id),
        firstName = firstName,
        lastName = lastName,
        email = email,
        billingPhone = "555-1234",
        billingAddress1 = "123 Main St",
        shippingAddress1 = "123 Main St",
    )

    private fun analyticsStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("total_sales", "170.35")
            put("gross_sales", "190.00")
            put("net_revenue", "120.15")
        },
        intervals = listOf(
            analyticsInterval("2026-05-01", "50.00", "35.00"),
            analyticsInterval("2026-05-02", "120.35", "85.15"),
        ),
    )

    private fun orderAnalyticsStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("orders_count", "42")
            put("avg_order_value", "85.30")
        },
        intervals = listOf(
            orderAnalyticsInterval("2026-05-01", "12", "80.10"),
            orderAnalyticsInterval("2026-05-02", "30", "87.38"),
        ),
    )

    private fun analyticsInterval(
        interval: String,
        totalSales: String,
        netRevenue: String,
    ) = buildJsonObject {
        put("interval", interval)
        put("date_start", "$interval 00:00:00")
        putJsonObject("subtotals") {
            put("total_sales", totalSales)
            put("net_revenue", netRevenue)
        }
    }

    private fun orderAnalyticsInterval(
        interval: String,
        ordersCount: String,
        averageOrderValue: String,
    ) = buildJsonObject {
        put("interval", interval)
        put("date_start", "$interval 00:00:00")
        putJsonObject("subtotals") {
            put("orders_count", ordersCount)
            put("avg_order_value", averageOrderValue)
        }
    }

    private companion object {
        private const val PRODUCT_IMAGE_URL = "https://example.com/socks.png"
        private val ORDER_LINE_ITEMS_JSON = """
            [
              {"id":10,"name":"Socks","quantity":1,"total":"9.99"},
              {"id":11,"name":"Hat","quantity":2,"total":"12.00"}
            ]
        """.trimIndent()
        private const val ANALYTICS_STATS_ID =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:USD"
        private const val ANALYTICS_STATS_ID_NO_CURRENCY =
            "analytics_revenue:after:2026-05-01:before:2026-05-07:interval:day:currency:none"
        private const val ANALYTICS_ORDERS_STATS_ID =
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day:currency:none"
        private const val ANALYTICS_ORDERS_STATS_ID_NO_CURRENCY =
            "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day"
    }
}
