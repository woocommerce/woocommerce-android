package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiStreamChunk(
    val choices: List<OpenAiChoice>,
)

@Serializable
internal data class OpenAiChoice(
    val delta: OpenAiDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class OpenAiDelta(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCallDelta>? = null,
)

@Serializable
internal data class OpenAiToolCallDelta(
    val index: Int? = null,
    val id: String? = null,
    val function: OpenAiFunctionCallDelta? = null,
)

@Serializable
internal data class OpenAiFunctionCallDelta(
    val name: String? = null,
    val arguments: String? = null,
)
