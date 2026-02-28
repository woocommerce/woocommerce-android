package com.woocommerce.android.ui.ai.mcp

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Maps MCP tool definitions to OpenAI function calling format.
 *
 * The WooCommerce MCP server returns tool schemas derived from WordPress REST API schemas,
 * which contain non-standard fields that must be sanitized before sending to OpenAI:
 * - `"required": true/false` (WP per-property boolean) vs JSON Schema `"required": ["field"]`
 * - `"context"`, `"readonly"`, `"arg_options"` (WP-specific annotations)
 * - `"type": "array"` without `"items"` (OpenAI requires items for array types)
 *
 * Additionally, empty JSON objects `{}` must be avoided because the /jetpack-ai-query
 * backend uses PHP's json_decode($json, true) which converts `{}` into `[]` (empty array).
 */
object McpToolMapper {
    fun toOpenAITools(mcpTools: List<Tool>): List<OpenAIToolDefinition> {
        return mcpTools.map { tool ->
            OpenAIToolDefinition(
                function = OpenAIFunctionSchema(
                    name = tool.name,
                    description = tool.description ?: "",
                    parameters = buildParametersSchema(tool)
                )
            )
        }
    }

    private fun buildParametersSchema(tool: Tool): JsonObject {
        val inputSchema = tool.inputSchema
        val schema = buildJsonObject {
            put("type", "object")
            val props = inputSchema.properties
            if (props is JsonObject && props.isNotEmpty()) {
                put("properties", props)
            }
            inputSchema.required?.takeIf { it.isNotEmpty() }?.let { required ->
                put("required", JsonArray(required.map { JsonPrimitive(it) }))
            }
        }
        return sanitizeSchema(schema) as JsonObject
    }

    /**
     * Recursively sanitizes a JSON Schema element for OpenAI compatibility.
     *
     * Removes WordPress-specific annotations and fixes type declarations
     * that would cause OpenAI validation errors.
     */
    private fun sanitizeSchema(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                val cleaned = buildJsonObject {
                    for ((key, value) in element) {
                        if (key in WP_SPECIFIC_KEYS) continue
                        if (key == "required" && value is JsonPrimitive && value.booleanOrNull != null) continue
                        put(key, sanitizeSchema(value))
                    }
                }
                fixArrayTypeWithoutItems(cleaned)
            }
            is JsonArray -> JsonArray(element.map { sanitizeSchema(it) })
            else -> element
        }
    }

    /**
     * OpenAI requires `"items"` when `"type"` includes `"array"`.
     * WP schemas often declare `"type": "array"` or `"type": [..., "array", ...]` without `"items"`.
     * This removes `"array"` from the type when items is missing.
     */
    private fun fixArrayTypeWithoutItems(obj: JsonObject): JsonObject {
        val typeElement = obj["type"] ?: return obj
        val hasItems = "items" in obj

        if (hasItems) return obj

        return when (typeElement) {
            // "type": "array" (single string) → replace with "string"
            is JsonPrimitive -> {
                if (typeElement.content == "array") {
                    rebuildWithType(obj, JsonPrimitive("string"))
                } else {
                    obj
                }
            }
            // "type": ["null", "string", ..., "array"] → remove "array" from the list
            is JsonArray -> {
                val types = typeElement.mapNotNull { (it as? JsonPrimitive)?.content }
                if ("array" !in types) return obj

                val filtered = types.filter { it != "array" }
                val newType = when {
                    filtered.isEmpty() -> JsonPrimitive("string")
                    filtered.size == 1 -> JsonPrimitive(filtered.first())
                    else -> JsonArray(filtered.map { JsonPrimitive(it) })
                }
                rebuildWithType(obj, newType)
            }
            else -> obj
        }
    }

    private fun rebuildWithType(obj: JsonObject, newType: JsonElement): JsonObject {
        return buildJsonObject {
            for ((key, value) in obj) {
                if (key == "type") {
                    put("type", newType)
                } else {
                    put(key, value)
                }
            }
        }
    }

    private val WP_SPECIFIC_KEYS = setOf("context", "readonly", "arg_options")
}

data class OpenAIToolDefinition(
    val type: String = "function",
    val function: OpenAIFunctionSchema
)

data class OpenAIFunctionSchema(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
