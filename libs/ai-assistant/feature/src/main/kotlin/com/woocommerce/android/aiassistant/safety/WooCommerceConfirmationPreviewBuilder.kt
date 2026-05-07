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
    fun build(
        request: ConfirmationRequest,
        snapshot: ConfirmationSnapshot? = null,
    ): ConfirmationPreview = build(request.toolName, request.arguments, snapshot)

    fun build(
        call: ToolCall,
        snapshot: ConfirmationSnapshot? = null,
    ): ConfirmationPreview = build(call.name, call.arguments, snapshot)

    fun build(
        toolName: String,
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot? = null,
    ): ConfirmationPreview = when (toolName) {
        ORDERS_UPDATE -> ordersUpdate(arguments, snapshot)
        ORDERS_BULK_UPDATE -> ordersBulkUpdate(arguments)
        PRODUCTS_UPDATE -> productsUpdate(arguments, snapshot)
        PRODUCTS_BULK_UPDATE -> productsBulkUpdate(arguments)
        PRODUCT_VARIATIONS_UPDATE -> productVariationsUpdate(arguments, snapshot)
        else -> genericPreview(toolName)
    }

    fun supportsDedicatedPreview(toolName: String): Boolean = toolName in DEDICATED_TOOL_NAMES

    private fun ordersUpdate(arguments: JsonObject, snapshot: ConfirmationSnapshot?): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_order_update_generic))
        val status = arguments.stringValue("status")
        val fields = buildList {
            status?.let {
                add(
                    textField(
                        name = "status",
                        value = it,
                        label = R.string.ai_assistant_confirmation_field_status,
                        beforeValue = snapshot?.currentValues?.get("status"),
                    )
                )
            }
            if (arguments.containsKey("customer_note")) {
                add(
                    messageField(
                        name = "customer_note",
                        value = string(R.string.ai_assistant_confirmation_field_value_updated),
                        label = R.string.ai_assistant_confirmation_field_customer_note,
                    )
                )
            }
            arguments.stringValue("billing_email")?.let {
                add(textField("billing_email", it, R.string.ai_assistant_confirmation_field_billing_email))
            }
        }

        return ConfirmationPreview(
            message = string(
                if (status.emailsCustomer()) {
                    R.string.ai_assistant_confirmation_order_update_summary
                } else {
                    R.string.ai_assistant_confirmation_order_update_title
                },
                raw(id.toString()),
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
            patch.stringValue("status")?.let { status ->
                add(textField("status", status, R.string.ai_assistant_confirmation_field_status))
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
        val emailsCustomer = patch.stringValue("status").emailsCustomer()
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = if (emailsCustomer) {
                    R.string.ai_assistant_confirmation_orders_bulk_update_summary_single
                } else {
                    R.string.ai_assistant_confirmation_orders_bulk_update_title_single
                },
                multiple = if (emailsCustomer) {
                    R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple
                } else {
                    R.string.ai_assistant_confirmation_orders_bulk_update_title_multiple
                },
            ),
            fields = fields,
            isBulk = true,
        )
    }

    private fun productsUpdate(arguments: JsonObject, snapshot: ConfirmationSnapshot?): ConfirmationPreview {
        val id = arguments.longValue("id")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_product_update_generic))
        val fields = productFields(arguments, snapshot)
        return ConfirmationPreview(
            message = productUpdateTitle(id, snapshot),
            fields = fields,
        )
    }

    private fun productsBulkUpdate(arguments: JsonObject): ConfirmationPreview {
        val ids = arguments.longArrayValue("ids")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val patch = arguments.objectValue("patch")
            ?: return ConfirmationPreview(string(R.string.ai_assistant_confirmation_products_bulk_update_generic))
        val fields = productFields(patch, snapshot = null)
        return ConfirmationPreview(
            message = quantity(
                quantity = ids.size,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
            ),
            fields = fields,
            isBulk = true,
        )
    }

    private fun productVariationsUpdate(arguments: JsonObject, snapshot: ConfirmationSnapshot?): ConfirmationPreview {
        val productId = arguments.longValue("product_id")
            ?: return ConfirmationPreview(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
        val variationId = arguments.longValue("id")
            ?: return ConfirmationPreview(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
        val fields = variationFields(arguments, snapshot)
        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_product_variation_update_title,
                raw(variationId.toString()),
                raw(productId.toString()),
            ),
            fields = fields,
        )
    }

    private fun productFields(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
    ): List<ConfirmationPreviewField> =
        productOrVariationFields(
            arguments = arguments,
            snapshot = snapshot,
            includeName = true,
            includeStockStatus = false,
            includeSku = false,
        )

    private fun variationFields(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
    ): List<ConfirmationPreviewField> =
        productOrVariationFields(
            arguments = arguments,
            snapshot = snapshot,
            includeName = false,
            includeStockStatus = true,
            includeSku = true,
        )

    private fun productOrVariationFields(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
        includeName: Boolean,
        includeStockStatus: Boolean,
        includeSku: Boolean,
    ): List<ConfirmationPreviewField> = buildList {
        addOptionalNameField(arguments, snapshot, includeName)
        addOptionalTextField(
            arguments = arguments,
            snapshot = snapshot,
            key = "regular_price",
            label = R.string.ai_assistant_confirmation_field_regular_price,
        )
        addOptionalSalePriceField(arguments, snapshot)
        addOptionalStockQuantityField(arguments, snapshot)
        addOptionalTextField(
            arguments = arguments,
            snapshot = snapshot,
            key = "stock_status",
            label = R.string.ai_assistant_confirmation_field_stock_status,
            include = includeStockStatus,
        )
        addOptionalTextField(
            arguments = arguments,
            snapshot = snapshot,
            key = "status",
            label = R.string.ai_assistant_confirmation_field_status,
        )
        addOptionalTextField(
            arguments = arguments,
            snapshot = snapshot,
            key = "sku",
            label = R.string.ai_assistant_confirmation_field_sku,
            include = includeSku,
        )
    }

    private fun MutableList<ConfirmationPreviewField>.addOptionalNameField(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
        includeName: Boolean,
    ) {
        arguments.stringValue("name")
            ?.takeIf { includeName }
            ?.let { name ->
                add(
                    textField(
                        name = "name",
                        value = name,
                        label = R.string.ai_assistant_confirmation_field_name,
                        beforeValue = snapshot?.currentValues?.get("name"),
                    )
                )
            }
    }

    private fun MutableList<ConfirmationPreviewField>.addOptionalSalePriceField(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
    ) {
        arguments.stringValue("sale_price")?.let { salePrice ->
            val value = salePrice.takeIf { it.isNotEmpty() }?.let(::raw)
                ?: string(R.string.ai_assistant_confirmation_field_value_off)
            add(
                messageField(
                    name = "sale_price",
                    value = value,
                    label = R.string.ai_assistant_confirmation_field_sale_price,
                    beforeValue = snapshot?.currentValues?.get("sale_price")?.let(::salePriceValue),
                )
            )
        }
    }

    private fun MutableList<ConfirmationPreviewField>.addOptionalStockQuantityField(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
    ) {
        arguments.intValue("stock_quantity")?.let { stockQuantity ->
            add(
                textField(
                    name = "stock_quantity",
                    value = stockQuantity.toString(),
                    label = R.string.ai_assistant_confirmation_field_stock_quantity,
                    beforeValue = snapshot?.currentValues?.get("stock_quantity"),
                )
            )
        }
    }

    private fun MutableList<ConfirmationPreviewField>.addOptionalTextField(
        arguments: JsonObject,
        snapshot: ConfirmationSnapshot?,
        key: String,
        label: Int,
        include: Boolean = true,
    ) {
        arguments.stringValue(key)
            ?.takeIf { include }
            ?.let { value ->
                add(
                    textField(
                        name = key,
                        value = value,
                        label = label,
                        beforeValue = snapshot?.currentValues?.get(key),
                    )
                )
            }
    }

    private fun genericPreview(toolName: String): ConfirmationPreview =
        ConfirmationPreview(
            string(R.string.ai_assistant_confirmation_generic_tool_call, raw(toolName))
        )

    private fun productUpdateTitle(
        id: Long,
        snapshot: ConfirmationSnapshot?,
    ): ConfirmationPreviewText =
        snapshot?.currentValues?.get("name")
            ?.takeIf { it.isNotBlank() }
            ?.let { name ->
                string(
                    R.string.ai_assistant_confirmation_product_update_title_with_name,
                    raw(name),
                    raw(id.toString()),
                )
            } ?: string(R.string.ai_assistant_confirmation_product_update_title, raw(id.toString()))

    private fun textField(
        name: String,
        value: String,
        @StringRes label: Int,
        beforeValue: String? = null,
    ): ConfirmationPreviewField = messageField(
        name = name,
        value = raw(value),
        label = label,
        beforeValue = beforeValue?.let { previewValue(name, it) },
    )

    private fun messageField(
        name: String,
        value: ConfirmationPreviewText,
        @StringRes label: Int,
        beforeValue: ConfirmationPreviewText? = null,
    ): ConfirmationPreviewField = ConfirmationPreviewField(
        name = name,
        value = value,
        label = string(label),
        beforeValue = beforeValue,
    )

    private fun salePriceValue(value: String): ConfirmationPreviewText =
        value.takeIf { it.isNotEmpty() }?.let(::raw)
            ?: string(R.string.ai_assistant_confirmation_field_value_off)

    private fun previewValue(name: String, value: String): ConfirmationPreviewText = when (name) {
        "sale_price" -> salePriceValue(value)
        else -> raw(value)
    }

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

    private fun String?.emailsCustomer(): Boolean =
        this?.let { it in CUSTOMER_NOTIFYING_STATUSES } == true

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
