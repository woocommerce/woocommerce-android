package com.woocommerce.android.aiassistant.chat.jetpackai

import com.woocommerce.android.aiassistant.chat.openai.OpenAiMessage
import com.woocommerce.android.aiassistant.chat.openai.OpenAiToolDefinition
import kotlinx.serialization.Serializable

@Serializable
internal data class JetpackAiQueryRequestBody(
    val feature: String,
    val model: String,
    val stream: Boolean,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiToolDefinition>? = null,
)
