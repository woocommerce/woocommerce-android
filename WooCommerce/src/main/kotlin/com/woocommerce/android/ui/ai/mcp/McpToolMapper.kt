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
     * Recursively removes non-JSON-Schema fields from the tool schema.
     *
     * WordPress REST API schemas contain annotations like `"context": ["view", "edit"]`,
     * `"readonly": true`, `"arg_options": {...}`, and `"required": true/false` (per-property
     * boolean). These are not valid JSON Schema and cause OpenAI validation errors.
     */
    private fun sanitizeSchema(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> buildJsonObject {
                for ((key, value) in element) {
                    if (key in WP_SPECIFIC_KEYS) continue
                    // "required" as boolean is a WP per-property annotation;
                    // JSON Schema only allows "required" as an array of field names at object level
                    if (key == "required" && value is JsonPrimitive && value.booleanOrNull != null) continue
                    put(key, sanitizeSchema(value))
                }
            }
            is JsonArray -> JsonArray(element.map { sanitizeSchema(it) })
            else -> element
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
