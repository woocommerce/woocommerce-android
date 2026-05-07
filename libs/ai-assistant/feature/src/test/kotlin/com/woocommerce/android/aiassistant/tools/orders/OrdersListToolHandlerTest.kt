package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersListToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersListToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun makeOrder(
        id: Long = 123L,
        status: String = "processing",
        total: String = "45.00",
        currency: String = "USD",
        number: String = "",
        dateCreated: String = "",
        customerId: Long = 0L,
        billingFirstName: String = "",
        billingLastName: String = "",
        billingEmail: String = "",
        billingPhone: String = "",
        billingCity: String = "",
        billingCountry: String = "",
        paymentMethodTitle: String = "",
        customerNote: String = "",
        datePaid: String = "",
        shippingTotal: String = "",
        discountTotal: String = "",
        shippingCity: String = "",
        shippingCountry: String = "",
        lineItems: String = "",
    ): OrderEntity = OrderEntity(
        localSiteId = LocalId(1),
        orderId = id,
        number = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        customerId = customerId,
        billingFirstName = billingFirstName,
        billingLastName = billingLastName,
        billingEmail = billingEmail,
        billingPhone = billingPhone,
        billingCity = billingCity,
        billingCountry = billingCountry,
        paymentMethodTitle = paymentMethodTitle,
        customerNote = customerNote,
        datePaid = datePaid,
        shippingTotal = shippingTotal,
        discountTotal = discountTotal,
        shippingCity = shippingCity,
        shippingCountry = shippingCountry,
        lineItems = lineItems,
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "list_orders", arguments = arguments)

    @Test
    fun `given orders are returned, when execute is called, then structured JSON contains count and ids`() =
        runTest {
            val order1 = makeOrder(id = 10L, status = "processing", total = "45.00")
            val order2 = makeOrder(id = 20L, status = "completed", total = "120.00")
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = listOf(order1, order2), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val json = (result as ToolResult.Success).structured.jsonObject

            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(2)
            val ids = requireNotNull(json["ids"]).jsonArray.map { it.jsonPrimitive.content.toLong() }
            assertThat(ids).containsExactly(10L, 20L)
        }

    @Test
    fun `given orders are returned, when execute is called, then structured JSON contains compact order rows`() =
        runTest {
            val order = makeOrder(
                id = 10L,
                status = "processing",
                total = "45.00",
                currency = "USD",
                number = "1010",
                dateCreated = "2026-05-01T10:00:00Z",
                customerId = 44L,
                billingFirstName = "Jane",
                billingLastName = "Doe",
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = listOf(order), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("orders").jsonArray.single().jsonObject
            assertThat(row.getValue("id").jsonPrimitive.long).isEqualTo(10L)
            assertThat(row.getValue("number").jsonPrimitive.content).isEqualTo("1010")
            assertThat(row.getValue("status").jsonPrimitive.content).isEqualTo("processing")
            assertThat(row.getValue("total").jsonPrimitive.content).isEqualTo("45.00")
            assertThat(row.getValue("currency").jsonPrimitive.content).isEqualTo("USD")
            assertThat(row.getValue("date_created").jsonPrimitive.content).isEqualTo("2026-05-01T10:00:00Z")
            assertThat(row.getValue("customer_id").jsonPrimitive.long).isEqualTo(44L)
            assertThat(row.getValue("customer_name").jsonPrimitive.content).isEqualTo("Jane Doe")
        }

    @Test
    fun `given order list extra fields, when execute is called, then every allowed list extra is included`() =
        runTest {
            val order = makeOrder(
                id = 10L,
                status = "processing",
                total = "45.00",
                currency = "USD",
                number = "1010",
                paymentMethodTitle = "Credit Card",
                billingFirstName = "Jane",
                billingLastName = "Doe",
                billingEmail = "jane@example.com",
                billingPhone = "555-0100",
                billingCity = "Portland",
                billingCountry = "US",
                shippingCity = "Seattle",
                shippingCountry = "US",
                customerNote = "Leave at the desk",
                datePaid = "2026-05-01T10:30:00Z",
                shippingTotal = "6.00",
                discountTotal = "2.00",
                lineItems = """
                    [{"id":1,"name":"Socks","quantity":1,"sku":"SOCK","total":"12.00","product_id":7,"variation_id":0}]
                """.trimIndent(),
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = listOf(order), canLoadMore = false))
            )

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        putJsonArray("extra_fields") {
                            add("billing")
                            add("payment_method_title")
                            add("customer_email")
                            add("line_items")
                            add("customer_note")
                            add("date_paid")
                            add("shipping_total")
                            add("discount_total")
                            add("shipping")
                        }
                    }
                )
            )

            val row = (result as ToolResult.Success).structured.jsonObject
                .getValue("orders").jsonArray.single().jsonObject
            assertOrderListExtras(row)
        }

    @Test
    fun `given unsupported order list extra field, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(buildJsonObject { putJsonArray("extra_fields") { add("metadata") } })
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason)
                .contains("Unsupported orders_list extra_fields: metadata")
            verify(dataSource, never()).fetchOrders(search = null)
        }

    @Test
    fun `given unknown order list argument, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { put("unexpected", true) }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason).contains("Unsupported orders_list argument")
            verify(dataSource, never()).fetchOrders(search = null)
        }

    @Test
    fun `given orders with mixed statuses, when execute is called, then status_counts is correct`() =
        runTest {
            val orders = listOf(
                makeOrder(id = 1L, status = "processing"),
                makeOrder(id = 2L, status = "processing"),
                makeOrder(id = 3L, status = "completed"),
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = orders, canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            val statusCounts = requireNotNull(json["status_counts"]).jsonObject
            assertThat(requireNotNull(statusCounts["processing"]).jsonPrimitive.int).isEqualTo(2)
            assertThat(requireNotNull(statusCounts["completed"]).jsonPrimitive.int).isEqualTo(1)
        }

    @Test
    fun `given orders, when execute is called, then total_range reflects min and max totals`() =
        runTest {
            val orders = listOf(
                makeOrder(id = 1L, total = "10.00", currency = "USD"),
                makeOrder(id = 2L, total = "480.00", currency = "USD"),
                makeOrder(id = 3L, total = "120.00", currency = "USD"),
            )
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = orders, canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            val totalRange = requireNotNull(json["total_range"]).jsonObject
            assertThat(requireNotNull(totalRange["min"]).jsonPrimitive.content).isEqualTo("10.00")
            assertThat(requireNotNull(totalRange["max"]).jsonPrimitive.content).isEqualTo("480.00")
            assertThat(requireNotNull(totalRange["currency"]).jsonPrimitive.content).isEqualTo("USD")
        }

    @Test
    fun `given empty result, when execute is called, then total_range is absent and count is zero`() =
        runTest {
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.success(AIOrdersDataSource.OrdersPage(orders = emptyList(), canLoadMore = false))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            val json = (result as ToolResult.Success).structured.jsonObject
            assertThat(requireNotNull(json["count"]).jsonPrimitive.int).isEqualTo(0)
            assertThat(json["total_range"]).isNull()
        }

    @Test
    fun `given data source fails, when execute is called, then a retryable TransportError is returned`() =
        runTest {
            whenever(dataSource.fetchOrders(search = null)).thenReturn(
                Result.failure(IllegalStateException("network error"))
            )

            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            val error = result as ToolResult.TransportError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.retryable).isTrue
        }

    @Test
    fun `given search argument is an integer, when execute is called, then a ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(buildJsonObject { put("search", 42) })
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.toolCallId).isEqualTo("call-1")
        }

    private fun assertOrderListExtras(row: JsonObject) {
        assertThat(row.getValue("payment_method_title").jsonPrimitive.content).isEqualTo("Credit Card")
        assertThat(row.getValue("customer_email").jsonPrimitive.content).isEqualTo("jane@example.com")
        assertThat(row.getValue("customer_note").jsonPrimitive.content).isEqualTo("Leave at the desk")
        assertThat(row.getValue("date_paid").jsonPrimitive.content).isEqualTo("2026-05-01T10:30:00Z")
        assertThat(row.getValue("shipping_total").jsonPrimitive.content).isEqualTo("6.00")
        assertThat(row.getValue("discount_total").jsonPrimitive.content).isEqualTo("2.00")
        assertThat(row.getValue("billing").jsonObject.getValue("phone").jsonPrimitive.content)
            .isEqualTo("555-0100")
        assertThat(row.getValue("shipping").jsonObject.getValue("city").jsonPrimitive.content)
            .isEqualTo("Seattle")
        assertThat(row.getValue("line_items_count").jsonPrimitive.int).isEqualTo(1)
        assertThat(row.getValue("line_items_truncated").jsonPrimitive.boolean).isFalse
        assertThat(row.getValue("line_items").jsonArray.single().jsonObject.getValue("sku").jsonPrimitive.content)
            .isEqualTo("SOCK")
    }
}
