package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class OpenAiToolDefinition(
    val type: String,
    val function: OpenAiFunctionDefinition,
)

@Serializable
internal data class OpenAiFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
internal data class OpenAiToolCall(
    val id: String,
    val type: String,
    val function: OpenAiFunctionCall,
)

@Serializable
internal data class OpenAiFunctionCall(
    val name: String,
    val arguments: String,
)
