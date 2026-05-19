package com.woocommerce.android.aiassistant.safety

import androidx.annotation.StringRes
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

internal fun raw(value: String) = ConfirmationPreviewText.Raw(value)

internal fun string(
    @StringRes id: Int,
    vararg args: ConfirmationPreviewText,
) = ConfirmationPreviewText.Resource(id, args.toList())

internal fun quantity(
    quantity: Int,
    @StringRes singular: Int,
    @StringRes multiple: Int,
    vararg args: ConfirmationPreviewText,
) = ConfirmationPreviewText.Quantity(quantity, singular, multiple, args.toList())

internal fun textField(
    name: String,
    value: String,
    @StringRes label: Int,
    beforeValue: String? = null,
): ConfirmationPreviewField = messageField(
    name = name,
    value = raw(value),
    label = label,
    beforeValue = beforeValue?.let(::raw),
)

internal fun messageField(
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

internal fun JsonObject.stringValue(name: String): String? =
    this[name]?.asJsonPrimitiveOrNull()?.contentOrNull

internal fun JsonObject.intValue(name: String): Int? =
    this[name]?.asJsonPrimitiveOrNull()?.intOrNull

internal fun JsonObject.longValue(name: String): Long? =
    runCatching { this[name]?.jsonPrimitive }.getOrNull()?.longOrNull

internal fun JsonObject.arrayValue(name: String): JsonArray? =
    this[name]?.asJsonArrayOrNull()

internal fun JsonObject.longArrayValue(name: String): List<Long>? {
    val array = arrayValue(name) ?: return null
    return array.map {
        it.asJsonPrimitiveOrNull()?.longOrNull ?: return null
    }
}

internal fun JsonObject.objectValue(name: String): JsonObject? =
    this[name]?.asJsonObjectOrNull()

internal fun Double.formatStockQuantity(): String =
    if (rem(1.0) == 0.0) toLong().toString() else toString()

private fun JsonElement.asJsonArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? = runCatching { jsonPrimitive }.getOrNull()
