package com.woocommerce.android.ui.ai.mcp

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Maps MCP tool definitions to OpenAI function calling format
 * for use with the AI API Proxy's chat/completions endpoint.
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
            inputSchema.properties?.let { props ->
                put("properties", props)
            } ?: putJsonObject("properties") {}
            inputSchema.required?.let { required ->
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
