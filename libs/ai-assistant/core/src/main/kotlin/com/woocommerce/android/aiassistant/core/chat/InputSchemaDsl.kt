package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@DslMarker
annotation class InputSchemaMarker

@InputSchemaMarker
class InputSchemaBuilder {
    private val properties = mutableMapOf<String, JsonObject>()
    private val requiredKeys = mutableListOf<String>()

    fun string(name: String, description: String? = null, required: Boolean = false) =
        prop(name, "string", description, required)

    fun integer(name: String, description: String? = null, required: Boolean = false) =
        prop(name, "integer", description, required)

    fun boolean(name: String, description: String? = null, required: Boolean = false) =
        prop(name, "boolean", description, required)

    fun enum(name: String, values: List<String>, description: String? = null, required: Boolean = false) {
        properties[name] = buildJsonObject {
            put("type", "string")
            description?.let { put("description", it) }
            putJsonArray("enum") { values.forEach { add(it) } }
        }
        if (required) requiredKeys += name
    }

    fun array(name: String, itemType: String = "string", description: String? = null, required: Boolean = false) {
        properties[name] = buildJsonObject {
            put("type", "array")
            putJsonObject("items") { put("type", itemType) }
            description?.let { put("description", it) }
        }
        if (required) requiredKeys += name
    }

    fun arrayEnum(
        name: String,
        values: List<String>,
        description: String? = null,
        required: Boolean = false
    ) {
        properties[name] = buildJsonObject {
            put("type", "array")
            description?.let { put("description", it) }
            putJsonObject("items") {
                put("type", "string")
                putJsonArray("enum") { values.forEach { add(it) } }
            }
        }
        if (required) requiredKeys += name
    }

    fun objectProperty(
        name: String,
        description: String? = null,
        required: Boolean = false,
        block: InputSchemaBuilder.() -> Unit
    ) {
        properties[name] = InputSchemaBuilder().apply(block).build(description)
        if (required) requiredKeys += name
    }

    private fun prop(name: String, type: String, description: String?, required: Boolean) {
        properties[name] = buildJsonObject {
            put("type", type)
            description?.let { put("description", it) }
        }
        if (required) requiredKeys += name
    }

    internal fun build(description: String? = null): JsonObject = buildJsonObject {
        put("type", "object")
        description?.let { put("description", it) }
        put("additionalProperties", false)
        putJsonObject("properties") { properties.forEach { (k, v) -> put(k, v) } }
        if (requiredKeys.isNotEmpty()) {
            putJsonArray("required") { requiredKeys.forEach { add(it) } }
        }
    }
}

fun inputSchema(block: InputSchemaBuilder.() -> Unit): JsonObject =
    InputSchemaBuilder().apply(block).build()
