package com.woocommerce.android.aiassistant.chat.woomobileai

import com.woocommerce.android.aiassistant.chat.openai.OpenAiMessage
import com.woocommerce.android.aiassistant.chat.openai.OpenAiToolDefinition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WooMobileAiRequestEnvelope(
    val model: String,
    val stream: Boolean,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiToolDefinition>? = null,
    @SerialName("stream_options") val streamOptions: WooMobileAiStreamOptions,
)

@Serializable
internal data class WooMobileAiStreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean,
)
