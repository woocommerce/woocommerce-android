package com.woocommerce.android.aiassistant.tools.customers

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.ToolFailureDiagnosticsFactory
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject

internal class CustomersListToolHandler @Inject constructor(
    private val dataSource: AICustomersDataSource,
    private val diagnosticsFactory: ToolFailureDiagnosticsFactory,
) : AssistantToolHandler {
    override val descriptor = ToolDescriptor(
        name = "customers_list",
        description = "List customers or look up known customer IDs by passing include as an array of IDs. " +
            "Returns compact matches with id, first_name, last_name, email, username, and date_created.",
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                stringProperty("search", "Search customers by name, username, or similar customer text.")
                stringProperty("email", "Filter customers by email address.")
                putJsonObject("include") {
                    put("type", "array")
                    put("description", "Customer IDs to return from the list endpoint.")
                    putJsonObject("items") {
                        put("type", "integer")
                        put("minimum", 1)
                    }
                }
                putJsonObject("orderby") {
                    put("type", "string")
                    put("description", "Customer sort field.")
                    putJsonArray("enum") {
                        add("registered_date")
                        add("name")
                        add("id")
                        add("email")
                    }
                }
                putJsonObject("order") {
                    put("type", "string")
                    put("description", "Sort direction.")
                    putJsonArray("enum") {
                        add("asc")
                        add("desc")
                    }
                }
                putJsonObject("page") {
                    put("type", "integer")
                    put("minimum", 1)
                    put("description", "Results page. Only sent to WooCommerce when greater than 1.")
                }
                putJsonObject("per_page") {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", MAX_PER_PAGE)
                    put("description", "Results per page. Values are clamped to 1..50.")
                }
            }
            put("additionalProperties", false)
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = try {
            call.arguments.toCustomerListArgs()
        } catch (e: IllegalArgumentException) {
            return ToolResult.ValidationError(call.id, e.message ?: "Invalid customers_list arguments")
        }

        val result = dataSource.fetchCustomers(
            search = args.search,
            email = args.email,
            include = args.include,
            orderby = args.orderby,
            order = args.order,
            page = args.page,
            perPage = args.perPage,
        )

        return result.fold(
            onSuccess = { customers ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = customers.toStructuredSummary(),
                    uiStructured = null,
                )
            },
            onFailure = { error ->
                when (error) {
                    AICustomersDataSource.NoSelectedSiteException -> {
                        ToolResult.ValidationError(call.id, "No selected site")
                    }
                    else -> diagnosticsFactory.transportError(
                        toolCallId = call.id,
                        toolName = descriptor.name,
                        error = error,
                        retryable = error.isRetryableStoreError(),
                    )
                }
            }
        )
    }

    private fun Throwable.isRetryableStoreError(): Boolean {
        val wooError = (this as? OnChangedException)?.error as? WooError
            ?: return true

        return wooError.type in RETRYABLE_WOO_ERROR_TYPES
    }

    private fun JsonObject.toCustomerListArgs(): CustomerListArgs {
        validateAllowedArguments(this, ALLOWED_KEYS, "customers_list").getOrThrow()

        val orderby = stringArg("orderby") ?: DEFAULT_ORDERBY
        require(orderby in ALLOWED_ORDERBY) {
            "orderby must be one of: ${ALLOWED_ORDERBY.joinToString(", ")}"
        }

        val order = stringArg("order") ?: DEFAULT_ORDER
        require(order in ALLOWED_ORDER) {
            "order must be one of: ${ALLOWED_ORDER.joinToString(", ")}"
        }

        val pageValue = intArg("page")
        require(pageValue == null || pageValue >= 1) {
            "page must be greater than or equal to 1"
        }

        val perPageValue = intArg("per_page") ?: DEFAULT_PER_PAGE

        return CustomerListArgs(
            search = stringArg("search"),
            email = stringArg("email"),
            include = includeArg(),
            orderby = orderby,
            order = order,
            page = pageValue?.takeIf { it > 1 },
            perPage = perPageValue.coerceIn(MIN_PER_PAGE, MAX_PER_PAGE),
        )
    }

    private fun JsonObject.stringArg(name: String): String? {
        val element = get(name) ?: return null
        val primitive = element as? JsonPrimitive
        require(primitive != null && primitive.isString) {
            "$name must be a string"
        }
        return primitive.content.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.intArg(name: String): Int? {
        val element = get(name) ?: return null
        val primitive = element as? JsonPrimitive
        require(primitive != null) {
            "$name must be an integer"
        }
        return requireNotNull(primitive.intOrNull) {
            "$name must be an integer"
        }
    }

    private fun JsonObject.includeArg(): List<Long>? {
        val element = get("include") ?: return null
        require(element is JsonArray) {
            "include must be an array of customer IDs"
        }
        val ids = element.map { parseCustomerId(it.jsonPrimitive) }

        return ids.takeIf { it.isNotEmpty() }
    }

    private fun parseCustomerId(primitive: JsonPrimitive): Long {
        val id = requireNotNull(primitive.longOrNull) {
            "include must contain customer IDs"
        }
        require(id > 0) {
            "include must contain positive customer IDs"
        }
        return id
    }

    private fun List<WCCustomerModel>.toStructuredSummary() = buildJsonObject {
        put("count", size)
        putJsonArray("matches") {
            this@toStructuredSummary.forEach { customer ->
                add(
                    buildJsonObject {
                        put("id", customer.remoteCustomerId.value)
                        putOptionalString("first_name", customer.firstName)
                        putOptionalString("last_name", customer.lastName)
                        putOptionalString("email", customer.email)
                        putOptionalString("username", customer.username)
                        putOptionalString("date_created", customer.dateCreated)
                        putJsonObject("billing") {
                            putOptionalString("phone", customer.billingPhone)
                            putOptionalString("city", customer.billingCity)
                            putOptionalString("country", customer.billingCountry)
                        }
                        putJsonObject("shipping") {
                            putOptionalString("city", customer.shippingCity)
                            putOptionalString("country", customer.shippingCountry)
                        }
                        putOptionalString("role", customer.role)
                        putOptionalString("avatar_url", customer.avatarUrl)
                    }
                )
            }
        }
    }

    private fun JsonObjectBuilder.stringProperty(name: String, description: String) {
        putJsonObject(name) {
            put("type", "string")
            put("description", description)
        }
    }

    private fun JsonObjectBuilder.putOptionalString(name: String, value: String?) {
        if (!value.isNullOrBlank()) {
            put(name, value)
        }
    }

    private data class CustomerListArgs(
        val search: String?,
        val email: String?,
        val include: List<Long>?,
        val orderby: String,
        val order: String,
        val page: Int?,
        val perPage: Int,
    )

    private companion object {
        const val DEFAULT_ORDERBY = "registered_date"
        const val DEFAULT_ORDER = "desc"
        const val DEFAULT_PER_PAGE = 20
        const val MIN_PER_PAGE = 1
        const val MAX_PER_PAGE = 50

        val ALLOWED_KEYS = setOf(
            "search",
            "email",
            "include",
            "orderby",
            "order",
            "page",
            "per_page",
        )
        val ALLOWED_ORDERBY = setOf("registered_date", "name", "id", "email")
        val ALLOWED_ORDER = setOf("asc", "desc")
        val RETRYABLE_WOO_ERROR_TYPES = setOf(
            WooErrorType.TIMEOUT,
            WooErrorType.NO_CONNECTION,
        )
    }
}
