package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiRequestBody(
    val model: String,
    val stream: Boolean,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiToolDefinition>? = null,
    @SerialName("stream_options") val streamOptions: OpenAiStreamOptions? = null,
)

@Serializable
internal data class OpenAiStreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean,
)
