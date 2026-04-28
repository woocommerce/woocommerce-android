package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiRequestBody(
    val feature: String,
    val model: String,
    val stream: Boolean,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiToolDefinition>? = null,
)
