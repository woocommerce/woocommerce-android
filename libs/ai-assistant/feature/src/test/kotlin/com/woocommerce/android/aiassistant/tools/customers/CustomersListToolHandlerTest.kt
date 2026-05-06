package com.woocommerce.android.aiassistant.tools.customers

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.StubToolHandler
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

class CustomersListToolHandlerTest {
    private val dataSource: AICustomersDataSource = mock()
    private val handler = CustomersListToolHandler(dataSource)

    @Test
    fun `when descriptor is inspected, then it exposes iOS-compatible schema and is safe`() {
        val descriptor = handler.descriptor
        val properties = descriptor.inputSchema.getValue("properties").jsonObject

        assertThat(descriptor.name).isEqualTo("customers_list")
        assertThat(descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.SAFE)
        assertThat(handler).isInstanceOf(AssistantToolHandler::class.java)
        assertThat(handler).isNotInstanceOf(StubToolHandler::class.java)
        assertThat(descriptor.description).contains("include")
        assertThat(descriptor.description).contains("known customer IDs")
        assertThat(descriptor.description).contains("id", "first_name", "last_name", "email")
        assertThat(properties.keys).containsExactlyInAnyOrder(
            "search",
            "email",
            "include",
            "orderby",
            "order",
            "page",
            "per_page",
        )
        assertThat(descriptor.inputSchema.getValue("additionalProperties").jsonPrimitive.content).isEqualTo("false")
        assertThat(properties.getValue("orderby").jsonObject.getValue("enum").jsonArray.stringValues())
            .containsExactly("registered_date", "name", "id", "email")
        assertThat(properties.getValue("order").jsonObject.getValue("enum").jsonArray.stringValues())
            .containsExactly("asc", "desc")
    }

    @Test
    fun `given no args, when executed, then default customer list is fetched`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.success(emptyList()))

        // when
        handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        verify(dataSource).fetchCustomers()
    }

    @Test
    fun `given include and bounded pagination args, when executed, then list endpoint lookup args are used`() =
        runTest {
            // given
            whenever(
                dataSource.fetchCustomers(
                    search = "jo",
                    email = "jane@example.com",
                    include = listOf(42, 73),
                    orderby = "email",
                    order = "asc",
                    page = null,
                    perPage = 50,
                )
            ).thenReturn(Result.success(emptyList()))

            // when
            handler.execute(
                toolCall(
                    arguments = buildJsonObject {
                        put("search", "jo")
                        put("email", "jane@example.com")
                        put(
                            "include",
                            buildJsonArray {
                                add(42)
                                add(73)
                            }
                        )
                        put("orderby", "email")
                        put("order", "asc")
                        put("page", 1)
                        put("per_page", 500)
                    }
                )
            )

            // then
            verify(dataSource).fetchCustomers(
                search = "jo",
                email = "jane@example.com",
                include = listOf(42, 73),
                orderby = "email",
                order = "asc",
                page = null,
                perPage = 50,
            )
        }

    @Test
    fun `given fetched customers, when executed, then compact structured summary is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(
            Result.success(
                listOf(
                    customer(id = 42, firstName = "Jane", lastName = "Doe", email = "jane@example.com"),
                    customer(id = 73, firstName = "", lastName = "", email = ""),
                )
            )
        )

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        result as ToolResult.Success
        assertThat(result.uiStructured).isNull()

        val structured = result.structured.jsonObject
        val matches = structured.getValue("matches").jsonArray
        assertThat(structured.getValue("count").jsonPrimitive.int).isEqualTo(2)
        assertThat(matches[0].jsonObject.getValue("id").jsonPrimitive.int).isEqualTo(42)
        assertThat(matches[0].jsonObject.getValue("first_name").jsonPrimitive.content).isEqualTo("Jane")
        assertThat(matches[0].jsonObject.getValue("last_name").jsonPrimitive.content).isEqualTo("Doe")
        assertThat(matches[0].jsonObject.getValue("email").jsonPrimitive.content).isEqualTo("jane@example.com")
        assertThat(matches[1].jsonObject.keys).containsExactly("id")
        assertThat(result.structured.toString()).doesNotContain("phone", "billing", "shipping", "analytics")
    }

    @Test
    fun `given no selected site, when executed, then validation error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(
            Result.failure(AICustomersDataSource.NoSelectedSiteException)
        )

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given unknown arg, when executed, then validation error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.success(emptyList()))

        // when
        val result = handler.execute(
            toolCall(
                arguments = buildJsonObject {
                    put("unexpected", "value")
                }
            )
        )

        // then
        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).fetchCustomers(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `given invalid include ID, when executed, then validation error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.success(emptyList()))

        // when
        val result = handler.execute(
            toolCall(
                arguments = buildJsonObject {
                    put("include", buildJsonArray { add(0) })
                }
            )
        )

        // then
        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).fetchCustomers(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `given non-array include, when executed, then validation error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.success(emptyList()))

        // when
        val result = handler.execute(
            toolCall(
                arguments = buildJsonObject {
                    put("include", 42)
                }
            )
        )

        // then
        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).fetchCustomers(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `given retryable store errors, when executed, then retryable transport error is returned`() = runTest {
        WOO_RETRYABLE_ERRORS.forEach { error ->
            // given
            whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

            // when
            val result = handler.execute(toolCall(arguments = buildJsonObject { }))

            // then
            assertThat(result).isEqualTo(ToolResult.TransportError(toolCallId = TOOL_CALL_ID, retryable = true))
        }
    }

    @Test
    fun `given nonretryable store errors, when executed, then nonretryable transport error is returned`() = runTest {
        WOO_NONRETRYABLE_ERRORS.forEach { error ->
            // given
            whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

            // when
            val result = handler.execute(toolCall(arguments = buildJsonObject { }))

            // then
            assertThat(result).isEqualTo(ToolResult.TransportError(toolCallId = TOOL_CALL_ID, retryable = false))
        }
    }

    @Test
    fun `given invalid response store error, when executed, then transport error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(TEST_ERROR)))

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertThat(result).isEqualTo(ToolResult.TransportError(toolCallId = TOOL_CALL_ID, retryable = false))
    }

    private fun toolCall(arguments: JsonObject) = ToolCall(
        id = TOOL_CALL_ID,
        name = "customers_list",
        arguments = arguments,
    )

    private fun customer(
        id: Long,
        firstName: String,
        lastName: String,
        email: String,
    ) = WCCustomerModel(
        localSiteId = LocalId(DEFAULT_SITE.id),
        remoteCustomerId = RemoteId(id),
        firstName = firstName,
        lastName = lastName,
        email = email,
        billingPhone = "555-1234",
        billingAddress1 = "123 Main St",
        shippingAddress1 = "123 Main St",
        analyticsCustomerId = 999,
    )

    private fun JsonArray.stringValues() = map { it.jsonPrimitive.contentOrNull }

    private companion object {
        const val TOOL_CALL_ID = "call_1"
        val DEFAULT_SITE = SiteModel().apply { id = 1 }
        val TEST_ERROR = WooError(WooErrorType.INVALID_RESPONSE, NETWORK_ERROR, "Network error")
        val WOO_RETRYABLE_ERRORS = listOf(
            WooError(WooErrorType.TIMEOUT, TIMEOUT, "Timed out"),
            WooError(WooErrorType.NO_CONNECTION, NO_CONNECTION, "No connection"),
        )
        val WOO_NONRETRYABLE_ERRORS = listOf(
            WooError(WooErrorType.AUTHORIZATION_REQUIRED, AUTHORIZATION_REQUIRED, "Unauthorized"),
            WooError(WooErrorType.API_ERROR, SERVER_ERROR, "Server error"),
        )
    }
}
