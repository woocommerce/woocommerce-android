package com.woocommerce.android.ui.ai.mcp

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Maps MCP tool definitions to OpenAI function calling format.
 *
 * Empty JSON objects `{}` in the schema must be avoided because the /jetpack-ai-query
 * backend uses PHP's json_decode($json, true) which converts `{}` into `[]` (empty array),
 * causing OpenAI to reject the schema with "[] is not of type 'object'".
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
        return buildJsonObject {
            put("type", "object")
            val props = inputSchema.properties
            if (props is JsonObject && props.isNotEmpty()) {
                put("properties", props)
            }
            inputSchema.required?.takeIf { it.isNotEmpty() }?.let { required ->
                put("required", JsonArray(required.map { JsonPrimitive(it) }))
            }
        }
    }
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
