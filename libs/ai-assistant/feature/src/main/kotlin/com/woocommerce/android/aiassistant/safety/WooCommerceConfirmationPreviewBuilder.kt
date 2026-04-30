package com.woocommerce.android.aiassistant.safety

import androidx.annotation.StringRes
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject

internal class WooCommerceConfirmationPreviewBuilder @Inject constructor() {
    fun build(request: ConfirmationRequest): ConfirmationPreview = build(request.toolName, request.arguments)

    fun build(call: ToolCall): ConfirmationPreview = build(call.name, call.arguments)

    fun build(
        toolName: String,
        arguments: JsonObject,
    ): ConfirmationPreview = when (toolName) {
        ORDERS_UPDATE -> ordersUpdate(arguments)
        ORDERS_BULK_UPDATE -> ordersBulkUpdate(arguments)
        PRODUCTS_UPDATE -> productsUpdate(arguments)
        PRODUCTS_BULK_UPDATE -> productsBulkUpdate(arguments)
        PRODUCT_VARIATIONS_UPDATE -> productVariationsUpdate(arguments)
        else -> genericPreview(toolName)
    }

    fun supportsDedicatedPreview(toolName: String): Boolean = toolName in DEDICATED_TOOL_NAMES

    private fun ordersUpdate(arguments: JsonObject): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_order_update_generic))
        val status = arguments.stringValue("status")
        val fields = buildList {
            status?.let { add(textField("status", it, R.string.ai_assistant_confirmation_field_status)) }
        }

        if (status != null) {
            val message = if (status in CUSTOMER_NOTIFYING_STATUSES) {
                string(
                    R.string.ai_assistant_confirmation_order_set_status_emails_customer,
                    raw(id.toString()),
                    raw(status),
                )
            } else {
                string(R.string.ai_assistant_confirmation_order_set_status, raw(id.toString()), raw(status))
            }
            return ConfirmationPreview(message = message, fields = fields)
        }

        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_order_update_summary,
                raw(id.toString()),
                fields.toChangeSummary(),
            ),
            fields = fields,
        )
    }

    private fun ordersBulkUpdate(arguments: JsonObject): ConfirmationPreview {
        val ids = arguments.longArrayValue("ids")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_orders_bulk_update_generic))
        val patch = arguments.objectValue("patch")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_orders_bulk_update_generic))
        val fields = buildList {
            patch.stringValue("status")?.let {
                add(textField("status", it, R.string.ai_assistant_confirmation_field_status))
            }
            if (patch.containsKey("customer_note")) {
                add(
                    messageField(
                        name = "customer_note",
                        value = string(R.string.ai_assistant_confirmation_field_value_updated),
                        label = R.string.ai_assistant_confirmation_field_customer_note,
                    )
                )
            }
            patch.stringValue("billing_email")?.let {
                add(textField("billing_email", it, R.string.ai_assistant_confirmation_field_billing_email))
            }
        }
        val summary = fields.toChangeSummary(
            statusEmailImpact = patch.stringValue("status")
                ?.takeIf { it in CUSTOMER_NOTIFYING_STATUSES }
                ?.let {
                    if (ids.size == 1) {
                        R.string.ai_assistant_confirmation_change_summary_status_emails_customer
                    } else {
                        R.string.ai_assistant_confirmation_change_summary_status_emails_customers
                    }
                },
        )
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                summary,
            ),
            fields = fields,
        )
    }

    private fun productsUpdate(arguments: JsonObject): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_update_generic))
        val fields = productFields(arguments)
        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_update_summary,
                raw(id.toString()),
                fields.toChangeSummary(),
            ),
            fields = fields,
        )
    }

    private fun productsBulkUpdate(arguments: JsonObject): ConfirmationPreview {
        val ids = arguments.longArrayValue("ids")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val patch = arguments.objectValue("patch")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val fields = productFields(patch)
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_summary_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_summary_multiple,
                fields.toChangeSummary(),
            ),
            fields = fields,
        )
    }

    private fun productVariationsUpdate(arguments: JsonObject): ConfirmationPreview {
        val productId = arguments.longValue("product_id")
            ?: return ConfirmationPreview(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
        val variationId = arguments.longValue("id")
            ?: return ConfirmationPreview(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
        val fields = variationFields(arguments)
        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_variation_update_summary,
                raw(variationId.toString()),
                raw(productId.toString()),
                fields.toChangeSummary(),
            ),
            fields = fields,
        )
    }

    private fun productFields(arguments: JsonObject): List<ConfirmationPreviewField> =
        productOrVariationFields(arguments, includeName = true, includeStockStatus = false, includeSku = false)

    private fun variationFields(arguments: JsonObject): List<ConfirmationPreviewField> =
        productOrVariationFields(arguments, includeName = false, includeStockStatus = true, includeSku = true)

    private fun productOrVariationFields(
        arguments: JsonObject,
        includeName: Boolean,
        includeStockStatus: Boolean,
        includeSku: Boolean,
    ): List<ConfirmationPreviewField> = buildList {
        arguments.stringValue("name")
            ?.takeIf { includeName }
            ?.let { add(textField("name", it, R.string.ai_assistant_confirmation_field_name)) }
        arguments.stringValue("regular_price")
            ?.let { add(textField("regular_price", it, R.string.ai_assistant_confirmation_field_regular_price)) }
        arguments.stringValue("sale_price")
            ?.let {
                val value = it.takeIf { price -> price.isNotEmpty() }?.let(::raw)
                    ?: string(R.string.ai_assistant_confirmation_field_value_off)
                add(messageField("sale_price", value, R.string.ai_assistant_confirmation_field_sale_price))
            }
        arguments.intValue("stock_quantity")
            ?.let {
                add(
                    textField(
                        name = "stock_quantity",
                        value = it.toString(),
                        label = R.string.ai_assistant_confirmation_field_stock_quantity,
                    )
                )
            }
        arguments.stringValue("stock_status")
            ?.takeIf { includeStockStatus }
            ?.let { add(textField("stock_status", it, R.string.ai_assistant_confirmation_field_stock_status)) }
        arguments.stringValue("status")
            ?.let { add(textField("status", it, R.string.ai_assistant_confirmation_field_status)) }
        arguments.stringValue("sku")
            ?.takeIf { includeSku }
            ?.let { add(textField("sku", it, R.string.ai_assistant_confirmation_field_sku)) }
    }

    private fun genericPreview(toolName: String): ConfirmationPreview =
        ConfirmationPreview(
            string(R.string.ai_assistant_confirmation_generic_tool_call, raw(toolName))
        )

    private fun List<ConfirmationPreviewField>.toChangeSummary(
        @StringRes statusEmailImpact: Int? = null,
    ): ConfirmationPreviewText {
        if (isEmpty()) {
            return string(R.string.ai_assistant_confirmation_change_summary_empty)
        }

        return map { field ->
            when (field.name) {
                "regular_price" -> string(
                    R.string.ai_assistant_confirmation_change_summary_regular_price,
                    field.value,
                )
                "sale_price" -> string(R.string.ai_assistant_confirmation_change_summary_sale_price, field.value)
                "stock_quantity" -> string(
                    R.string.ai_assistant_confirmation_change_summary_stock_quantity,
                    field.value,
                )
                "stock_status" -> string(
                    R.string.ai_assistant_confirmation_change_summary_stock_status,
                    field.value,
                )
                "sku" -> string(R.string.ai_assistant_confirmation_change_summary_sku, field.value)
                "customer_note" -> string(R.string.ai_assistant_confirmation_change_summary_customer_note)
                "billing_email" -> string(
                    R.string.ai_assistant_confirmation_change_summary_billing_email,
                    field.value,
                )
                "status" -> string(
                    statusEmailImpact ?: R.string.ai_assistant_confirmation_change_summary_status,
                    field.value,
                )
                else -> string(R.string.ai_assistant_confirmation_change_summary_field, raw(field.name), field.value)
            }
        }.toLocalizedList()
    }

    private fun textField(
        name: String,
        value: String,
        @StringRes label: Int,
    ): ConfirmationPreviewField = messageField(
        name = name,
        value = raw(value),
        label = label,
    )

    private fun messageField(
        name: String,
        value: ConfirmationPreviewText,
        @StringRes label: Int,
    ): ConfirmationPreviewField = ConfirmationPreviewField(
        name = name,
        value = value,
        label = string(label),
    )

    private fun List<ConfirmationPreviewText>.toLocalizedList(): ConfirmationPreviewText =
        reduceOrNull { left, right ->
            string(R.string.ai_assistant_confirmation_message_list_separator, left, right)
        } ?: string(R.string.ai_assistant_confirmation_change_summary_empty)

    private fun raw(value: String) = ConfirmationPreviewText.Raw(value)

    private fun string(
        @StringRes id: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Resource(id, args.toList())

    private fun quantity(
        quantity: Int,
        @StringRes singular: Int,
        @StringRes multiple: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Quantity(quantity, singular, multiple, args.toList())

    private fun JsonObject.stringValue(name: String): String? =
        this[name]?.asJsonPrimitiveOrNull()?.contentOrNull

    private fun JsonObject.intValue(name: String): Int? =
        this[name]?.asJsonPrimitiveOrNull()?.intOrNull

    private fun JsonObject.longValue(name: String): Long? =
        this[name]?.asJsonPrimitiveOrNull()?.longOrNull

    private fun JsonObject.arrayValue(name: String): JsonArray? =
        this[name]?.asJsonArrayOrNull()

    private fun JsonObject.longArrayValue(name: String): List<Long>? {
        val array = arrayValue(name) ?: return null
        return array.map {
            it.asJsonPrimitiveOrNull()?.longOrNull ?: return null
        }
    }

    private fun JsonObject.objectValue(name: String): JsonObject? =
        this[name]?.asJsonObjectOrNull()

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? = runCatching { jsonPrimitive }.getOrNull()

    private companion object {
        const val ORDERS_UPDATE = "orders_update"
        const val ORDERS_BULK_UPDATE = "orders_bulk_update"
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCTS_BULK_UPDATE = "products_bulk_update"
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"

        val DEDICATED_TOOL_NAMES = setOf(
            ORDERS_UPDATE,
            ORDERS_BULK_UPDATE,
            PRODUCTS_UPDATE,
            PRODUCTS_BULK_UPDATE,
            PRODUCT_VARIATIONS_UPDATE,
        )

        val CUSTOMER_NOTIFYING_STATUSES = setOf(
            "processing",
            "completed",
            "cancelled",
            "refunded",
            "on-hold",
        )
    }
}
