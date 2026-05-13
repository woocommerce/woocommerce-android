package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import kotlinx.serialization.json.JsonObject

internal fun productFields(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
): List<ConfirmationPreviewField> =
    productOrVariationFields(
        arguments = arguments,
        currentValues = currentValues,
        includeName = true,
        includeStockStatus = false,
        includeSku = false,
    )

internal fun variationFields(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
): List<ConfirmationPreviewField> =
    productOrVariationFields(
        arguments = arguments,
        currentValues = currentValues,
        includeName = false,
        includeStockStatus = true,
        includeSku = true,
    )

internal fun productUpdateTitle(
    id: Long,
    currentValues: Map<String, String>?,
): ConfirmationPreviewText =
    currentValues?.get("name")
        ?.takeIf { it.isNotBlank() }
        ?.let { name ->
            string(
                R.string.ai_assistant_confirmation_product_update_title_with_name,
                raw(name),
                raw(id.toString()),
            )
        } ?: string(R.string.ai_assistant_confirmation_product_update_title, raw(id.toString()))

private fun productOrVariationFields(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
    includeName: Boolean,
    includeStockStatus: Boolean,
    includeSku: Boolean,
): List<ConfirmationPreviewField> = buildList {
    addOptionalNameField(arguments, currentValues, includeName)
    addOptionalTextField(
        arguments = arguments,
        currentValues = currentValues,
        key = "regular_price",
        label = R.string.ai_assistant_confirmation_field_regular_price,
    )
    addOptionalSalePriceField(arguments, currentValues)
    addOptionalStockQuantityField(arguments, currentValues)
    addOptionalTextField(
        arguments = arguments,
        currentValues = currentValues,
        key = "stock_status",
        label = R.string.ai_assistant_confirmation_field_stock_status,
        include = includeStockStatus,
    )
    addOptionalTextField(
        arguments = arguments,
        currentValues = currentValues,
        key = "status",
        label = R.string.ai_assistant_confirmation_field_status,
    )
    addOptionalTextField(
        arguments = arguments,
        currentValues = currentValues,
        key = "sku",
        label = R.string.ai_assistant_confirmation_field_sku,
        include = includeSku,
    )
}

private fun MutableList<ConfirmationPreviewField>.addOptionalNameField(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
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
                    beforeValue = currentValues?.get("name"),
                )
            )
        }
}

private fun MutableList<ConfirmationPreviewField>.addOptionalSalePriceField(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
) {
    arguments.stringValue("sale_price")?.let { salePrice ->
        add(
            messageField(
                name = "sale_price",
                value = salePriceValue(salePrice),
                label = R.string.ai_assistant_confirmation_field_sale_price,
                beforeValue = currentValues?.get("sale_price")?.let(::salePriceValue),
            )
        )
    }
}

private fun MutableList<ConfirmationPreviewField>.addOptionalStockQuantityField(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
) {
    arguments.intValue("stock_quantity")?.let { stockQuantity ->
        add(
            textField(
                name = "stock_quantity",
                value = stockQuantity.toString(),
                label = R.string.ai_assistant_confirmation_field_stock_quantity,
                beforeValue = currentValues?.get("stock_quantity"),
            )
        )
    }
}

private fun MutableList<ConfirmationPreviewField>.addOptionalTextField(
    arguments: JsonObject,
    currentValues: Map<String, String>?,
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
                    beforeValue = currentValues?.get(key),
                )
            )
        }
}

private fun salePriceValue(value: String): ConfirmationPreviewText =
    value.takeIf { it.isNotEmpty() }?.let(::raw)
        ?: string(R.string.ai_assistant_confirmation_field_value_off)
