package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonObject

/**
 * OpenAI-style function definition for the tools the model is allowed to call.
 * `parameters` is the JSON Schema describing the call shape.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)
