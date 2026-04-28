package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class ChatCompletionRequestPayload(
    val feature: String,
    val stream: Boolean,
    val model: String,
    val messages: List<ChatCompletionMessagePayload>,
    val tools: List<ChatCompletionToolPayload>? = null,
) {
    companion object {
        fun from(
            request: ChatRequest,
            feature: String,
            model: String,
            stream: Boolean = true,
        ): ChatCompletionRequestPayload = ChatCompletionRequestPayload(
            feature = feature,
            stream = stream,
            model = model,
            messages = request.messages.map(AssistantMessage::toPayload),
            tools = request.tools.takeIf { it.isNotEmpty() }?.map(ToolDefinition::toPayload),
        )
    }
}

@Serializable
internal data class ChatCompletionMessagePayload(
    val role: String,
    val content: JsonElement,
    @SerialName("tool_calls") val toolCalls: List<ChatCompletionToolCallPayload>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
internal data class ChatCompletionToolPayload(
    val type: String,
    val function: ChatCompletionToolDefinitionPayload,
)

@Serializable
internal data class ChatCompletionToolDefinitionPayload(
    val name: String,
    val description: String,
    val parameters: JsonElement,
)

@Serializable
internal data class ChatCompletionToolCallPayload(
    val id: String,
    val type: String,
    val function: ChatCompletionFunctionCallPayload,
)

@Serializable
internal data class ChatCompletionFunctionCallPayload(
    val name: String,
    val arguments: String,
)

@Serializable
internal data class ChatCompletionStreamChunkPayload(
    val choices: List<ChatCompletionChoicePayload>,
)

@Serializable
internal data class ChatCompletionChoicePayload(
    val delta: ChatCompletionDeltaPayload? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class ChatCompletionDeltaPayload(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ChatCompletionToolCallDeltaPayload>? = null,
)

@Serializable
internal data class ChatCompletionToolCallDeltaPayload(
    val index: Int? = null,
    val id: String? = null,
    val function: ChatCompletionFunctionCallDeltaPayload? = null,
)

@Serializable
internal data class ChatCompletionFunctionCallDeltaPayload(
    val name: String? = null,
    val arguments: String? = null,
)

private fun AssistantMessage.toPayload(): ChatCompletionMessagePayload = when (this) {
    is AssistantMessage.System -> ChatCompletionMessagePayload(
        role = role,
        content = JsonPrimitive(content),
    )
    is AssistantMessage.User -> ChatCompletionMessagePayload(
        role = role,
        content = JsonPrimitive(content),
    )
    is AssistantMessage.Assistant -> ChatCompletionMessagePayload(
        role = role,
        content = content?.let(::JsonPrimitive) ?: JsonNull,
        toolCalls = toolCalls.takeIf { it.isNotEmpty() }?.map(ToolCall::toPayload),
    )
    is AssistantMessage.Tool -> ChatCompletionMessagePayload(
        role = role,
        content = JsonPrimitive(content),
        toolCallId = toolCallId,
    )
}

private fun ToolDefinition.toPayload(): ChatCompletionToolPayload = ChatCompletionToolPayload(
    type = FUNCTION_TYPE,
    function = ChatCompletionToolDefinitionPayload(
        name = name,
        description = description,
        parameters = parameters,
    ),
)

private fun ToolCall.toPayload(): ChatCompletionToolCallPayload = ChatCompletionToolCallPayload(
    id = id,
    type = FUNCTION_TYPE,
    function = ChatCompletionFunctionCallPayload(
        name = name,
        arguments = arguments.toString(),
    ),
)

private const val FUNCTION_TYPE = "function"
