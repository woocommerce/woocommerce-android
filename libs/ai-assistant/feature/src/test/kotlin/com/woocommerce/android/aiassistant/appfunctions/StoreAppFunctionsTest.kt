package com.woocommerce.android.aiassistant.appfunctions

import android.app.PendingIntent
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionNotSupportedException
import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.orders.CompactOrderLineItem
import com.woocommerce.android.aiassistant.tools.orders.OrderDetailResponse
import com.woocommerce.android.aiassistant.tools.orders.OrderListRowResponse
import com.woocommerce.android.aiassistant.tools.products.CompactVariationAttribute
import com.woocommerce.android.aiassistant.tools.products.ProductDetailResponse
import com.woocommerce.android.aiassistant.tools.products.ProductListRowResponse
import com.woocommerce.android.aiassistant.tools.products.ProductVariationDetailResponse
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoreAppFunctionsTest {
    private var allowed = true
    private var openStore = store()
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doAnswer { openStore }
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val intents: StoreAppFunctionIntents = mock()
    private val pendingIntent: PendingIntent = mock()
    private val storeAccess = AppFunctionsStoreAccess(selectedSite, AppFunctionsAccessPolicy { allowed })
    private val json = assistantJsonForTests()

    @Before
    fun setUp() {
        runBlocking {
            whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(settings(CURRENCY))
        }
        whenever(intents.confirmation(any(), any(), any())).thenReturn(pendingIntent)
        whenever(intents.openOrder(any(), any(), any())).thenReturn(pendingIntent)
    }

    @Test
    fun `given access is not allowed, when a function runs, then it fails as not supported without a tool call`() =
        runTest {
            // GIVEN
            allowed = false
            val registry = Registry { ToolResult.ValidationError(it.id, "should not run") }

            // WHEN
            val error = runCatching { functions(registry).findOrders(null, null, null) }.exceptionOrNull()

            // THEN
            assertThat(error).isInstanceOf(AppFunctionNotSupportedException::class.java)
            assertThat(registry.calls).isEmpty()
        }

    @Test
    fun `given the store changes during a suspended tool call, when orders are found, then no result is returned`() =
        runTest {
            // GIVEN
            val registry = Registry {
                yield()
                openStore = store(id = OTHER_SITE_ID, url = OTHER_STORE_URL, name = OTHER_STORE_NAME)
                success(orderListPayload(emptyList()))
            }

            // WHEN
            val error = runCatching { functions(registry).findOrders(null, null, null) }.exceptionOrNull()

            // THEN
            assertThat(error).isInstanceOf(AppFunctionNotSupportedException::class.java)
        }

    @Test
    fun `given today and a store five hours behind UTC, when sales are read, then the store date is used`() = runTest {
        // GIVEN
        val registry = Registry {
            success(buildJsonObject { put("totals", JsonNull) })
        }

        // WHEN
        functions(registry).getSalesSummary(PERIOD_TODAY, Instant.parse("2026-11-08T03:00:00Z"))

        // THEN
        assertThat(registry.calls.single().arguments["after"]?.jsonPrimitive?.content).isEqualTo("2026-11-07")
        assertThat(registry.calls.single().arguments["before"]?.jsonPrimitive?.content).isEqualTo("2026-11-07")
        assertThat(registry.calls.single().arguments["interval"]?.jsonPrimitive?.content).isEqualTo("day")
    }

    @Test
    fun `given each sales period, when totals are requested, then its date range is sent`() = runTest {
        val registry = Registry { success(buildJsonObject { put("totals", JsonNull) }) }
        val today = Instant.parse("2026-11-08T18:00:00Z")

        val yesterday = functions(registry).getSalesSummary(PERIOD_YESTERDAY, today)
        assertThat(yesterday.startDate).isEqualTo("2026-11-07")
        assertThat(yesterday.endDate).isEqualTo("2026-11-07")
        val week = functions(registry).getSalesSummary(PERIOD_LAST_7_DAYS, today)
        assertThat(week.startDate).isEqualTo("2026-11-02")
        assertThat(week.endDate).isEqualTo("2026-11-08")
        val month = functions(registry).getSalesSummary(PERIOD_THIS_MONTH, today)
        assertThat(month.startDate).isEqualTo("2026-11-01")
        assertThat(month.endDate).isEqualTo("2026-11-08")
    }

    @Test
    fun `given an unknown period, when sales are requested, then it fails before calling a tool`() = runTest {
        val registry = Registry { ToolResult.ValidationError(it.id, "unused") }
        val error = runCatching { functions(registry).getSalesSummary("quarter") }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given analytics totals, when sales are read, then totals and currency are mapped`() = runTest {
        val registry = Registry {
            success(
                buildJsonObject {
                    put(
                        "totals",
                        buildJsonObject {
                            put("net_revenue", "123.45")
                            put("orders_count", 6)
                            put("avg_order_value", "20.58")
                            put("num_items_sold", 9)
                        },
                    )
                },
            )
        }

        val summary = functions(registry).getSalesSummary(PERIOD_TODAY, Instant.parse("2026-11-08T12:00:00Z"))

        assertThat(summary.storeName).isEqualTo(STORE_NAME)
        assertThat(summary.currency).isEqualTo(CURRENCY)
        assertThat(summary.netSales).isEqualTo("123.45")
        assertThat(summary.orderCount).isEqualTo(6)
        assertThat(summary.averageOrderValue).isEqualTo("20.58")
        assertThat(summary.itemsSold).isEqualTo(9)
    }

    @Test
    fun `given analytics has no totals, when sales are read, then metrics are null`() = runTest {
        val registry = Registry { success(buildJsonObject { put("totals", JsonNull) }) }

        val summary = functions(registry).getSalesSummary(PERIOD_TODAY)

        assertThat(summary.netSales).isNull()
        assertThat(summary.orderCount).isNull()
        assertThat(summary.averageOrderValue).isNull()
        assertThat(summary.itemsSold).isNull()
    }

    @Test
    fun `given the sales tool fails, when sales are read, then the error has no diagnostics`() = runTest {
        val registry = Registry { ToolResult.TransportError(it.id, retryable = true) }
        val error = runCatching { functions(registry).getSalesSummary(PERIOD_TODAY) }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionAppUnknownException::class.java)
        assertThat(error?.message).contains("Couldn't load sales").doesNotContain("status", "payload", "diagnostics")
    }

    @Test
    fun `given a tool validation error, when orders are found, then its reason becomes invalid argument`() = runTest {
        val registry = Registry { ToolResult.ValidationError(it.id, "status was invalid") }
        val error = runCatching { functions(registry).findOrders(null, null, null) }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
        assertThat(error?.message).isEqualTo("status was invalid")
    }

    @Test
    fun `given order filters and no limit, when orders are found, then ten orders are requested`() = runTest {
        val registry = Registry { success(orderListPayload(emptyList())) }

        functions(registry).findOrders("Taylor", "processing", null)

        val arguments = registry.calls.single().arguments
        assertThat(arguments["per_page"]?.jsonPrimitive?.int).isEqualTo(DEFAULT_LIMIT)
        assertThat(arguments["search"]?.jsonPrimitive?.content).isEqualTo("Taylor")
        assertThat(arguments["status"]?.jsonPrimitive?.content).isEqualTo("processing")
    }

    @Test
    fun `given a limit above fifty, when orders are found, then the limit is clamped`() = runTest {
        val registry = Registry { success(orderListPayload(emptyList())) }

        functions(registry).findOrders(null, null, 99)

        assertThat(registry.calls.single().arguments["per_page"]?.jsonPrimitive?.int).isEqualTo(MAX_LIMIT)
    }

    @Test
    fun `given matching orders, when orders are found, then the summary includes customer name only`() = runTest {
        val row = orderRow()
        val registry = Registry { success(orderListPayload(listOf(row))) }

        val result = functions(registry).findOrders(null, null, 1)

        assertThat(result.orders.single()).isEqualTo(row.toSummary())
        assertThat(result.mayHaveMore).isTrue()
    }

    @Test
    fun `given an order, when details are read, then items and totals are mapped without contact fields`() = runTest {
        val order = orderDetail(
            lineItems = listOf(
                CompactOrderLineItem(
                    name = "Tote",
                    quantity = 2f,
                    total = "24.00",
                    productId = 0,
                    variationId = 0,
                ),
            ),
            datePaid = "",
        )
        val registry = Registry { success(json.encodeToJsonElement(order)) }

        val details = functions(registry).getOrderDetails(ORDER_ID)

        assertThat(details.datePaid).isNull()
        assertThat(details.itemCount).isEqualTo(2)
        assertThat(details.items.single().name).isEqualTo("Tote")
        assertThat(details.items.single().quantity).isEqualTo(2f)
        assertThat(details.items.single().productId).isNull()
        assertThat(details.items.single().variationId).isNull()
        assertThat(details.customerName).isEqualTo(CUSTOMER_NAME)
    }

    @Test
    fun `given an order lookup fails and the list confirms absence, when details are read, then it is not found`() =
        runTest {
            val registry = Registry { call ->
                when (call.name) {
                    ORDERS_GET -> ToolResult.TransportError(call.id, retryable = true)
                    else -> success(buildJsonObject { put("count", 0) })
                }
            }

            val error = runCatching { functions(registry).getOrderDetails(ORDER_ID) }.exceptionOrNull()

            assertThat(error).isInstanceOf(AppFunctionElementNotFoundException::class.java)
            assertThat(error?.message).contains(STORE_NAME, ORDER_ID.toString())
        }

    @Test
    fun `given a failed order lookup but the list still contains the id, when details are read, then it is app unknown`() =
        runTest {
            val registry = Registry { call ->
                if (call.name == ORDERS_GET) {
                    ToolResult.TransportError(call.id, retryable = true)
                } else {
                    success(buildJsonObject { put("count", 1) })
                }
            }

            val error = runCatching { functions(registry).getOrderDetails(ORDER_ID) }.exceptionOrNull()

            assertThat(error).isInstanceOf(AppFunctionAppUnknownException::class.java)
            assertThat(error?.message).contains("Couldn't load order")
        }

    @Test
    fun `given both order lookups fail, when details are read, then it is app unknown`() = runTest {
        val registry = Registry { ToolResult.TransportError(it.id, retryable = true) }
        val error = runCatching { functions(registry).getOrderDetails(ORDER_ID) }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionAppUnknownException::class.java)
    }

    @Test
    fun `given product filters and tracked and untracked products, when products are found, then filters and stock map`() =
        runTest {
            val registry = Registry {
                success(
                    buildJsonObject {
                        put(
                            "products",
                            json.encodeToJsonElement(
                                listOf(
                                    productRow(manageStock = true, stockQuantity = 6.8),
                                    productRow(id = OTHER_PRODUCT_ID, manageStock = false, stockQuantity = 4.0),
                                ),
                            ),
                        )
                        put("can_load_more", true)
                    },
                )
            }

            val result = functions(registry).findProducts("tote", "TOTE-1", "instock", 2)

            val arguments = registry.calls.single().arguments
            assertThat(arguments["search"]?.jsonPrimitive?.content).isEqualTo("tote")
            assertThat(arguments["sku"]?.jsonPrimitive?.content).isEqualTo("TOTE-1")
            assertThat(arguments["stock_status"]?.jsonPrimitive?.content).isEqualTo("instock")
            assertThat(result.products.map { it.stockQuantity }).containsExactly(6, null)
            assertThat(result.products.first().sku).isEqualTo("TOTE-1")
            assertThat(result.mayHaveMore).isTrue()
            assertThat(result.currency).isEqualTo(CURRENCY)
        }

    @Test
    fun `given a negative quantity, when stock change is requested, then no tool runs`() = runTest {
        val registry = Registry { ToolResult.ValidationError(it.id, "unused") }
        val error = runCatching { functions(registry).requestStockChange(PRODUCT_ID, null, -1) }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given a simple product, when stock change is requested, then a confirmation is created without writing`() =
        runTest {
            val registry = Registry {
                if (it.name == PRODUCTS_GET) {
                    success(json.encodeToJsonElement(productDetail()))
                } else {
                    ToolResult.ValidationError(it.id, "unexpected write")
                }
            }

            val result = functions(registry).requestStockChange(PRODUCT_ID, null, 8)
            val call = argumentCaptor<ToolCall>()
            val store = argumentCaptor<StoreIdentity>()

            verify(intents).confirmation(call.capture(), store.capture(), eqStoreName())
            assertThat(result).isSameAs(pendingIntent)
            assertThat(call.firstValue.name).isEqualTo(PRODUCTS_UPDATE)
            assertThat(call.firstValue.arguments["stock_quantity"]?.jsonPrimitive?.int).isEqualTo(8)
            assertThat(registry.calls.map { it.name }).containsExactly(PRODUCTS_GET)
            assertThat(store.firstValue).isEqualTo(openStore.identity())
        }

    @Test
    fun `given a variable product without variation id, when stock change is requested, then variations are listed`() =
        runTest {
            val registry = Registry { call ->
                when (call.name) {
                    PRODUCTS_GET -> success(json.encodeToJsonElement(productDetail(type = VARIABLE_PRODUCT_TYPE)))
                    else -> success(
                        buildJsonObject {
                            put("variations", json.encodeToJsonElement(listOf(variationDetail())))
                        },
                    )
                }
            }

            val error = runCatching { functions(registry).requestStockChange(PRODUCT_ID, null, 8) }.exceptionOrNull()

            assertThat(error).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
            assertThat(error?.message).contains(VARIATION_ID.toString(), "Large")
            verify(intents, never()).confirmation(any(), any(), any())
        }

    @Test
    fun `given an unsupported product type, when stock change is requested, then it fails as invalid argument`() =
        runTest {
            val registry = Registry { success(json.encodeToJsonElement(productDetail(type = "grouped"))) }

            val error = runCatching { functions(registry).requestStockChange(PRODUCT_ID, null, 8) }.exceptionOrNull()

            assertThat(error).isInstanceOf(AppFunctionInvalidArgumentException::class.java)
            assertThat(error?.message).contains("grouped product")
        }

    @Test
    fun `given a variation id, when stock change is requested, then the variation update is prepared`() = runTest {
        val registry = Registry { call ->
            if (call.name == PRODUCTS_GET) {
                success(json.encodeToJsonElement(productDetail(type = VARIABLE_PRODUCT_TYPE)))
            } else {
                success(buildJsonObject { put("id", VARIATION_ID) })
            }
        }

        functions(registry).requestStockChange(PRODUCT_ID, VARIATION_ID, 8)
        val call = argumentCaptor<ToolCall>()
        verify(intents).confirmation(call.capture(), any(), eqStoreName())

        assertThat(call.firstValue.name).isEqualTo(PRODUCT_VARIATIONS_UPDATE)
        assertThat(call.firstValue.arguments["product_id"]?.jsonPrimitive?.content).isEqualTo(PRODUCT_ID.toString())
        assertThat(registry.calls.map { it.name }).containsExactly(PRODUCTS_GET, PRODUCT_VARIATIONS_LIST)
    }

    @Test
    fun `given variation lookup fails, when stock change is requested, then no confirmation is returned`() = runTest {
        val registry = Registry { call ->
            if (call.name == PRODUCTS_GET) {
                success(json.encodeToJsonElement(productDetail(type = VARIABLE_PRODUCT_TYPE)))
            } else {
                ToolResult.TransportError(call.id, retryable = true)
            }
        }

        val appFunctions = functions(registry)
        val error = runCatching {
            appFunctions.requestStockChange(PRODUCT_ID, VARIATION_ID, 8)
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(AppFunctionAppUnknownException::class.java)
        verify(intents, never()).confirmation(any(), any(), any())
    }

    @Test
    fun `given a failed product lookup and a confirming list with no product, when stock is requested, then not found`() =
        runTest {
            val registry = Registry { call ->
                if (call.name == PRODUCTS_GET) {
                    ToolResult.TransportError(call.id, retryable = true)
                } else {
                    success(buildJsonObject { put("count", 0) })
                }
            }

            val error = runCatching { functions(registry).requestStockChange(PRODUCT_ID, null, 8) }.exceptionOrNull()

            assertThat(error).isInstanceOf(AppFunctionElementNotFoundException::class.java)
        }

    @Test
    fun `given a valid order, when the order handoff is requested, then its pending intent is returned`() = runTest {
        val registry = Registry { success(buildJsonObject {}) }

        val result = functions(registry).openOrderInApp(ORDER_ID)

        verify(intents).openOrder(ORDER_ID, openStore.identity(), STORE_NAME)
        assertThat(result).isSameAs(pendingIntent)
    }

    private fun functions(registry: ToolRegistry) = StoreAppFunctions(
        toolRegistry = registry,
        storeAccess = storeAccess,
        wooCommerceStore = wooCommerceStore,
        intents = intents,
        json = json,
    )

    private fun success(value: JsonElement) = ToolResult.Success(toolCallId = "call", structured = value)

    private fun orderListPayload(rows: List<OrderListRowResponse>) = buildJsonObject {
        put("orders", json.encodeToJsonElement(rows))
    }

    private fun orderRow() = OrderListRowResponse(
        id = ORDER_ID,
        number = ORDER_NUMBER,
        status = "processing",
        total = "42.50",
        currency = CURRENCY,
        dateCreated = "2026-11-08T12:00:00",
        customerId = 9L,
        customerName = CUSTOMER_NAME,
        customerEmail = "private@example.com",
    )

    private fun OrderListRowResponse.toSummary() = OrderSummary(
        orderId = id,
        orderNumber = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        customerName = customerName,
    )

    private fun orderDetail(
        lineItems: List<CompactOrderLineItem> = emptyList(),
        datePaid: String = "2026-11-08T12:05:00",
    ) = OrderDetailResponse(
        id = ORDER_ID,
        number = ORDER_NUMBER,
        status = "processing",
        total = "42.50",
        currency = CURRENCY,
        dateCreated = "2026-11-08T12:00:00",
        dateModified = "2026-11-08T12:00:00",
        paymentMethodTitle = "Card",
        customerName = CUSTOMER_NAME,
        customerEmail = "private@example.com",
        customerId = 9,
        datePaid = datePaid,
        customerNote = "private note",
        totalTax = "2.50",
        shippingTotal = "5.00",
        discountTotal = "1.00",
        lineItemsCount = 2,
        lineItemsTruncated = false,
        lineItems = lineItems,
    )

    private fun productRow(
        id: Long = PRODUCT_ID,
        manageStock: Boolean,
        stockQuantity: Double,
    ) = ProductListRowResponse(
        id = id,
        name = "Cotton Tote",
        sku = "TOTE-1",
        price = "12.00",
        stockStatus = "instock",
        type = "simple",
        status = "publish",
        stockQuantity = stockQuantity,
        manageStock = manageStock,
    )

    private fun productDetail(type: String = SIMPLE_PRODUCT_TYPE) = ProductDetailResponse(
        id = PRODUCT_ID,
        name = "Cotton Tote",
        status = "publish",
        type = type,
        sku = "TOTE-1",
        regularPrice = "12.00",
        salePrice = "",
        onSale = false,
        manageStock = true,
        stockQuantity = 4.0,
        stockStatus = "instock",
        dateCreated = "2026-01-01T00:00:00",
        totalSales = 2,
        parentId = 0,
        permalink = "https://bean.example/product/tote",
        variationsCount = 1,
        variationIds = listOf(VARIATION_ID),
        variationIdsTruncated = false,
    )

    private fun variationDetail() = ProductVariationDetailResponse(
        id = VARIATION_ID,
        productId = PRODUCT_ID,
        status = "publish",
        sku = "TOTE-L",
        regularPrice = "12.00",
        salePrice = "",
        onSale = false,
        manageStock = false,
        stockQuantity = 0.0,
        stockStatus = "instock",
        price = "12.00",
        attributes = listOf(CompactVariationAttribute(name = "Size", option = "Large")),
    )

    private fun store(id: Int = SITE_ID, url: String = STORE_URL, name: String = STORE_NAME) = SiteModel().apply {
        this.id = id
        this.url = url
        this.name = name
        displayName = ""
        timezone = "-5"
    }

    private fun settings(currency: String) = Settings(
        currencyCode = currency,
        currencyPosition = CurrencyPosition.LEFT,
        currencyThousandSeparator = ",",
        currencyDecimalSeparator = ".",
        currencyDecimalNumber = 2,
        countryCode = "US",
        stateCode = "CA",
        address = "",
        address2 = "",
        city = "",
        postalCode = "",
        couponsEnabled = true,
    )

    private class Registry(
        private val answer: suspend (ToolCall) -> ToolResult,
    ) : ToolRegistry {
        val calls = mutableListOf<ToolCall>()

        override fun descriptors() = emptyList<ToolDescriptor>()

        override suspend fun execute(call: ToolCall): ToolResult {
            calls += call
            return answer(call)
        }
    }

    private fun eqStoreName() = eq(STORE_NAME)

    private companion object {
        const val SITE_ID = 7
        const val OTHER_SITE_ID = 8
        const val PRODUCT_ID = 12L
        const val OTHER_PRODUCT_ID = 13L
        const val VARIATION_ID = 18L
        const val ORDER_ID = 42L
        const val ORDER_NUMBER = "1042"
        const val STORE_URL = "https://bean.example"
        const val OTHER_STORE_URL = "https://other.example"
        const val STORE_NAME = "Bean Shop"
        const val OTHER_STORE_NAME = "Other Shop"
        const val CUSTOMER_NAME = "Taylor Merchant"
        const val CURRENCY = "USD"
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
        const val SIMPLE_PRODUCT_TYPE = "simple"
        const val VARIABLE_PRODUCT_TYPE = "variable"
        const val ORDERS_GET = "orders_get"
        const val PRODUCTS_GET = "products_get"
        const val PRODUCTS_LIST = "products_list"
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCT_VARIATIONS_LIST = "product_variations_list"
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
    }
}
