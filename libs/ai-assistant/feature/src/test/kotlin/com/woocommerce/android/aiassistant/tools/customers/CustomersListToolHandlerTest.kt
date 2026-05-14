package com.woocommerce.android.aiassistant.tools.customers

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.StubToolHandler
import com.woocommerce.android.aiassistant.tools.testToolFailureDiagnosticsFactory
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
import org.json.JSONObject
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
    private val handler = CustomersListToolHandler(dataSource, testToolFailureDiagnosticsFactory())

    @Test
    fun `when descriptor is inspected, then it exposes iOS-compatible schema and is safe`() {
        val descriptor = handler.descriptor
        val properties = descriptor.inputSchema.getValue("properties").jsonObject

        assertThat(descriptor.name).isEqualTo("customers_list")
        assertThat(descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.SAFE)
        assertThat(handler).isInstanceOf(AssistantToolHandler::class.java)
        assertThat(handler).isNotInstanceOf(StubToolHandler::class.java)
        assertThat(descriptor.description).contains("optionally filtered by keyword")
        assertThat(descriptor.description).contains("Use `include=[id]` to look up one customer by ID")
        assertThat(descriptor.description).contains("After calling, pass results to `show_cards`")
        assertThat(descriptor.description).contains("do not retry with")
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
        assertThat(properties.getValue("search").jsonObject.getValue("description").jsonPrimitive.content)
            .isEqualTo("Free-text search across name, email, username.")
        assertThat(properties.getValue("email").jsonObject.getValue("description").jsonPrimitive.content)
            .isEqualTo("Exact email lookup.")
        assertThat(properties.getValue("include").jsonObject.getValue("description").jsonPrimitive.content)
            .isEqualTo("Specific customer IDs to include.")
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
                    customer(
                        id = 42,
                        firstName = "Jane",
                        lastName = "Doe",
                        email = "jane@example.com",
                        billingPhone = "",
                    ),
                    customer(id = 73, firstName = "", lastName = "", email = "", billingPhone = ""),
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
        assertThat(matches[0].jsonObject.keys).containsExactlyInAnyOrder("id", "first_name", "last_name", "email")
        assertThat(matches[1].jsonObject.keys).containsExactlyInAnyOrder("id")
    }

    @Test
    fun `given fetched customers, when executed, then available default fields are returned and order totals are not fabricated`() =
        runTest {
            whenever(dataSource.fetchCustomers()).thenReturn(
                Result.success(
                    listOf(
                        customer(
                            id = 42,
                            firstName = "Jane",
                            lastName = "Doe",
                            email = "jane@example.com",
                            username = "jane",
                            dateCreated = "2026-05-01T10:00:00Z",
                            billingPhone = "",
                        )
                    )
                )
            )

            val result = handler.execute(toolCall(arguments = buildJsonObject { }))

            val match = (result as ToolResult.Success).structured.jsonObject
                .getValue("matches").jsonArray.single().jsonObject
            assertThat(match.getValue("username").jsonPrimitive.content).isEqualTo("jane")
            assertThat(match.getValue("date_created").jsonPrimitive.content).isEqualTo("2026-05-01T10:00:00Z")
            assertThat(match.keys).containsExactlyInAnyOrder(
                "id",
                "first_name",
                "last_name",
                "email",
                "username",
                "date_created",
            )
        }

    @Test
    fun `given customer expanded fields, when executed, then every customer field is returned`() = runTest {
        whenever(dataSource.fetchCustomers()).thenReturn(
            Result.success(
                listOf(
                    customer(
                        id = 42,
                        role = "customer",
                        avatarUrl = "https://example.com/avatar.jpg",
                        billingPhone = "555-1234",
                        billingCity = "Portland",
                        billingCountry = "US",
                        shippingCity = "Seattle",
                        shippingCountry = "US",
                    )
                )
            )
        )

        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        val match = (result as ToolResult.Success).structured.jsonObject
            .getValue("matches").jsonArray.single().jsonObject
        val billing = match.getValue("billing").jsonObject
        assertThat(billing.getValue("phone").jsonPrimitive.content).isEqualTo("555-1234")
        assertThat(billing.getValue("city").jsonPrimitive.content).isEqualTo("Portland")
        assertThat(billing.getValue("country").jsonPrimitive.content).isEqualTo("US")
        assertThat(match.getValue("shipping").jsonObject.getValue("city").jsonPrimitive.content).isEqualTo("Seattle")
        assertThat(match.getValue("role").jsonPrimitive.content).isEqualTo("customer")
        assertThat(match.getValue("avatar_url").jsonPrimitive.content).isEqualTo("https://example.com/avatar.jpg")
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
    fun `given unsupported date args, when executed, then validation error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.success(emptyList()))

        // when
        val result = handler.execute(
            toolCall(
                arguments = buildJsonObject {
                    put("after", "2026-05-01")
                    put("before", "2026-05-07")
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
            assertCustomerTransportError(result, retryable = true)
        }
    }

    @Test
    fun `given woo error with numeric status, when executed, then status diagnostics are returned`() = runTest {
        // given
        val error = WooError(
            type = WooErrorType.TIMEOUT,
            original = TIMEOUT,
            message = "Timed out",
            errorData = wooErrorData(hasStatus = true, status = 503),
        )
        whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertCustomerTransportError(result, retryable = true, httpStatus = 503)
        assertThat((result as ToolResult.TransportError).diagnostics.transport?.bodySnippet).isNull()
    }

    @Test
    fun `given woo error without status, when executed, then transport diagnostics are absent`() = runTest {
        // given
        val error = WooError(
            type = WooErrorType.API_ERROR,
            original = SERVER_ERROR,
            message = "Server error",
            errorData = wooErrorData(hasStatus = false),
        )
        whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertCustomerTransportError(result, retryable = false)
    }

    @Test
    fun `given woo error with non numeric status, when executed, then transport diagnostics are absent`() = runTest {
        // given
        val error = WooError(
            type = WooErrorType.API_ERROR,
            original = SERVER_ERROR,
            message = "Server error",
            errorData = wooErrorData(hasStatus = true, status = "503"),
        )
        whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertCustomerTransportError(result, retryable = false)
    }

    @Test
    fun `given nonretryable store errors, when executed, then nonretryable transport error is returned`() = runTest {
        WOO_NONRETRYABLE_ERRORS.forEach { error ->
            // given
            whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(error)))

            // when
            val result = handler.execute(toolCall(arguments = buildJsonObject { }))

            // then
            assertCustomerTransportError(result, retryable = false)
        }
    }

    @Test
    fun `given invalid response store error, when executed, then transport error is returned`() = runTest {
        // given
        whenever(dataSource.fetchCustomers()).thenReturn(Result.failure(OnChangedException(TEST_ERROR)))

        // when
        val result = handler.execute(toolCall(arguments = buildJsonObject { }))

        // then
        assertCustomerTransportError(result, retryable = false)
    }

    private fun toolCall(arguments: JsonObject) = ToolCall(
        id = TOOL_CALL_ID,
        name = "customers_list",
        arguments = arguments,
    )

    private fun customer(
        id: Long,
        firstName: String = "",
        lastName: String = "",
        email: String = "",
        username: String = "",
        dateCreated: String = "",
        role: String = "",
        avatarUrl: String = "",
        billingPhone: String = "555-1234",
        billingCity: String = "",
        billingCountry: String = "",
        shippingCity: String = "",
        shippingCountry: String = "",
    ) = WCCustomerModel(
        localSiteId = LocalId(DEFAULT_SITE.id),
        remoteCustomerId = RemoteId(id),
        avatarUrl = avatarUrl,
        dateCreated = dateCreated,
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = role,
        username = username,
        billingPhone = billingPhone,
        billingAddress1 = "123 Main St",
        billingCity = billingCity,
        billingCountry = billingCountry,
        shippingAddress1 = "123 Main St",
        shippingCity = shippingCity,
        shippingCountry = shippingCountry,
        analyticsCustomerId = 999,
    )

    private fun JsonArray.stringValues() = map { it.jsonPrimitive.contentOrNull }

    private fun assertCustomerTransportError(
        result: ToolResult,
        retryable: Boolean,
        httpStatus: Int? = null,
    ) {
        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        result as ToolResult.TransportError
        assertThat(result.toolCallId).isEqualTo(TOOL_CALL_ID)
        assertThat(result.retryable).isEqualTo(retryable)
        assertThat(result.diagnostics.tool?.toolName).isEqualTo("customers_list")
        if (httpStatus == null) {
            assertThat(result.diagnostics.transport).isNull()
        } else {
            assertThat(result.diagnostics.transport?.httpStatus).isEqualTo(httpStatus)
        }
    }

    private fun wooErrorData(
        hasStatus: Boolean,
        status: Any? = null,
    ) = mock<JSONObject>().apply {
        whenever(has("status")).thenReturn(hasStatus)
        whenever(opt("status")).thenReturn(status)
    }

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
