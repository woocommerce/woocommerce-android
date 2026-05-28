package com.woocommerce.android.aiassistant.tools.orders

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.testToolFailureDiagnosticsFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersGetToolHandlerTest {

    private val dataSource: AIOrdersDataSource = mock()
    private val handler = OrdersGetToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
        diagnosticsFactory = testToolFailureDiagnosticsFactory(),
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "orders_get", arguments = arguments)

    private fun makeOrder(
        couponLines: String = "",
        feeLines: String = "",
        taxLines: String = "",
    ): OrderEntity = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 123L,
        number = "123",
        status = "processing",
        total = "45.00",
        currency = "USD",
        dateModified = "2024-01-16T00:00:00",
        datePaid = "2024-01-15T01:00:00",
        customerNote = "Leave at desk",
        totalTax = "4.00",
        shippingTotal = "6.00",
        discountTotal = "2.00",
        customerId = 55L,
        billingFirstName = "John",
        billingLastName = "Doe",
        billingEmail = "john@example.com",
        paymentMethodTitle = "Credit Card",
        dateCreated = "2024-01-15T00:00:00",
        couponLines = couponLines,
        feeLines = feeLines,
        taxLines = taxLines,
    )

    @Test
    fun `given descriptor, when inspected, then iOS-aligned order detail guidance is exposed`() {
        val description = handler.descriptor.description
        val properties = handler.descriptor.inputSchema.getValue("properties").jsonObject

        assertThat(description).contains("line items")
        assertThat(description).contains("products")
        assertThat(description).contains("order contents")
        assertThat(description).contains("prior-turn order position/reference")
        assertThat(description).contains("cards do not show")
        assertThat(description).contains("customer_id can chain into customers_list")
        assertThat(description).contains("Do not call just to re-render an existing order card")
        assertThat(properties.keys).containsExactly("id")
        assertThat(handler.descriptor.inputSchema.getValue("required").jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("id")
    }

    @Test
    fun `given a valid id, when execute is called, then structured JSON contains iOS-aligned fields`() =
        runTest {
            val order = makeOrder()
            whenever(dataSource.getOrder(123L)).thenReturn(Result.success(order))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 123) }))

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val success = result as ToolResult.Success
            assertThat(success.toolCallId).isEqualTo("call-1")

            val json = success.structured.jsonObject
            assertThat(requireNotNull(json["id"]).jsonPrimitive.int).isEqualTo(123)
            assertThat(requireNotNull(json["number"]).jsonPrimitive.content).isEqualTo("123")
            assertThat(requireNotNull(json["status"]).jsonPrimitive.content).isEqualTo("processing")
            assertThat(requireNotNull(json["total"]).jsonPrimitive.content).isEqualTo("45.00")
            assertThat(requireNotNull(json["currency"]).jsonPrimitive.content).isEqualTo("USD")
            assertThat(requireNotNull(json["customer_name"]).jsonPrimitive.content).isEqualTo("John Doe")
            assertThat(requireNotNull(json["customer_email"]).jsonPrimitive.content).isEqualTo("john@example.com")
            assertThat(requireNotNull(json["date_created"]).jsonPrimitive.content).isEqualTo("2024-01-15T00:00:00")
            assertThat(requireNotNull(json["payment_method_title"]).jsonPrimitive.content).isEqualTo("Credit Card")
            assertThat(requireNotNull(json["customer_id"]).jsonPrimitive.int).isEqualTo(55)
            assertThat(requireNotNull(json["line_items_count"]).jsonPrimitive.int).isEqualTo(0)
        }

    @Test
    fun `given unknown arg, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("id", 123)
                    put("unexpected", "x")
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        assertThat((result as ToolResult.ValidationError).reason).contains("Unsupported orders_get argument")
    }

    @Test
    fun `given adjustment fields, when execute is called, then coupon fee and tax lines are returned`() = runTest {
        val order = makeOrder(
            couponLines = """[{"id":10,"code":"SAVE10","discount":"5.00","discount_tax":"0.50"}]""",
            feeLines = """[{"id":20,"name":"Rush","total":"7.00","total_tax":"0.70","tax_status":"taxable"}]""",
            taxLines = """
                [{"id":30,"rate_id":40,"rate_code":"US-CA","label":"CA Tax","tax_total":"3.00","shipping_tax_total":"0.30"}]
            """.trimIndent(),
        )
        whenever(dataSource.getOrder(123L)).thenReturn(Result.success(order))

        val result = handler.execute(toolCall(buildJsonObject { put("id", 123) }))

        val json = (result as ToolResult.Success).structured.jsonObject
        assertThat(json.getValue("coupon_lines").jsonArray.single().jsonObject.getValue("code").jsonPrimitive.content)
            .isEqualTo("SAVE10")
        assertThat(json.getValue("fee_lines").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
            .isEqualTo("Rush")
        assertThat(json.getValue("tax_lines").jsonArray.single().jsonObject.getValue("rate_code").jsonPrimitive.content)
            .isEqualTo("US-CA")
    }

    @Test
    fun `given missing id, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject {}))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            val error = result as ToolResult.ValidationError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.reason).isNotBlank()
        }

    @Test
    fun `given the data source fails with retained response, when execute is called, then diagnostics are returned`() =
        runTest {
            whenever(dataSource.getOrder(7L)).thenReturn(Result.failure(orderFailureWithResponse(statusCode = 409)))

            val result = handler.execute(toolCall(buildJsonObject { put("id", 7) }))

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            val error = result as ToolResult.TransportError
            assertThat(error.toolCallId).isEqualTo("call-1")
            assertThat(error.retryable).isTrue
            assertThat(error.diagnostics.tool?.toolName).isEqualTo("orders_get")
            assertThat(error.diagnostics.transport?.httpStatus).isEqualTo(409)
        }

    private fun orderFailureWithResponse(statusCode: Int): OnChangedException {
        val response = NetworkResponse(
            statusCode,
            """{"code":"rest_invalid_param","message":"Bearer secret"}""".toByteArray(),
            false,
            0,
            emptyList(),
        )
        val networkError = WPAPINetworkError(BaseNetworkError(VolleyError(response)))
        return OnChangedException(WCOrderStore.OrderError(networkError = networkError))
    }
}
