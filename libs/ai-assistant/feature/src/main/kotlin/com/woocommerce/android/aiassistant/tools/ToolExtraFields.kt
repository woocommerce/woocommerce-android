package com.woocommerce.android.aiassistant.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal fun validateAllowedArguments(
    args: JsonObject,
    allowed: Set<String>,
    toolName: String,
): Result<Unit> {
    val unknown = args.keys - allowed
    return if (unknown.isEmpty()) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Unsupported $toolName argument(s): ${unknown.joinToString(", ")}"))
    }
}

internal fun parseExtraFields(
    args: JsonObject,
    allowed: Set<String>,
    toolName: String,
): Result<Set<String>> = runCatching {
    val element = args["extra_fields"] ?: return@runCatching emptySet()
    require(element is JsonArray) { "extra_fields must be an array of strings" }

    val values = element.map { item ->
        val primitive = item as? JsonPrimitive
        require(primitive != null && primitive.isString) { "extra_fields must be an array of strings" }
        primitive.content
    }.distinct()

    val unsupported = values.filterNot { it in allowed }
    require(unsupported.isEmpty()) {
        "Unsupported $toolName extra_fields: ${unsupported.joinToString(", ")}. " +
            "Allowed values: ${allowed.joinToString(", ")}"
    }
    values.toSet()
}

internal fun JsonObjectBuilder.extraFieldsProperty(
    allowed: List<String>,
    description: String,
) {
    putJsonObject("extra_fields") {
        put("type", "array")
        put("description", description)
        putJsonObject("items") {
            put("type", "string")
            putJsonArray("enum") { allowed.forEach { add(it) } }
        }
    }
}
