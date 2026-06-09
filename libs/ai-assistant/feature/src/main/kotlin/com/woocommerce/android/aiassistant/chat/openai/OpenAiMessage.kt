package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("role")
internal sealed interface OpenAiMessage {
    @Serializable
    @SerialName("system")
    data class System(val content: String) : OpenAiMessage

    @Serializable
    @SerialName("user")
    data class User(val content: String) : OpenAiMessage

    @Serializable
    @SerialName("assistant")
    data class Assistant(
        val content: String?,
        @SerialName("tool_calls") val toolCalls: List<OpenAiToolCall>? = null,
    ) : OpenAiMessage

    @Serializable
    @SerialName("tool")
    data class Tool(
        @SerialName("tool_call_id") val toolCallId: String,
        val content: String,
    ) : OpenAiMessage
}
