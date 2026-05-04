package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.core.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition

internal fun ChatRequest.toOpenAi(): OpenAiRequestBody = OpenAiRequestBody(
    feature = AssistantConfig.FEATURE_NAME,
    model = AssistantConfig.MODEL_ID,
    stream = true,
    messages = messages.map(AssistantMessage::toOpenAi),
    tools = tools.takeIf { it.isNotEmpty() }?.map(ToolDefinition::toOpenAi),
)

internal fun AssistantMessage.toOpenAi(): OpenAiMessage = when (this) {
    is AssistantMessage.System -> OpenAiMessage.System(content = content)
    is AssistantMessage.User -> OpenAiMessage.User(content = content)
    is AssistantMessage.Assistant -> OpenAiMessage.Assistant(
        // Jetpack AI rejects assistant tool-call replay messages when content is omitted/null.
        // send an empty string instead.
        content = content ?: "",
        toolCalls = toolCalls.takeIf { it.isNotEmpty() }?.map(ToolCall::toOpenAi),
    )
    is AssistantMessage.Tool -> OpenAiMessage.Tool(
        toolCallId = toolCallId,
        content = content,
    )
}

internal fun ToolDefinition.toOpenAi(): OpenAiToolDefinition = OpenAiToolDefinition(
    type = FUNCTION_TYPE,
    function = OpenAiFunctionDefinition(
        name = name,
        description = description,
        parameters = parameters,
    ),
)

internal fun OpenAiStreamChunk.toEvents(): List<AssistantEvent> {
    val choice = choices.firstOrNull() ?: return emptyList()
    val events = mutableListOf<AssistantEvent>()
    val delta = choice.delta

    if (delta != null) {
        delta.content
            ?.takeIf { it.isNotEmpty() }
            ?.let { events += AssistantEvent.TextDelta(it) }

        delta.toolCalls?.forEach { toolCall ->
            val index = toolCall.index ?: return@forEach
            events += AssistantEvent.ToolCallDelta(
                index = index,
                id = toolCall.id,
                name = toolCall.function?.name,
                argumentsDelta = toolCall.function?.arguments,
            )
        }
    }

    choice.finishReason?.let { events += AssistantEvent.Finish(it.toFinishReason()) }
    return events
}

private fun ToolCall.toOpenAi(): OpenAiToolCall = OpenAiToolCall(
    id = id,
    type = FUNCTION_TYPE,
    function = OpenAiFunctionCall(
        name = name,
        // The transport expects tool arguments as a JSON-encoded string, not nested JSON.
        arguments = arguments.toString(),
    ),
)

private fun String.toFinishReason(): FinishReason = when (this) {
    "stop" -> FinishReason.STOP
    "tool_calls" -> FinishReason.TOOL_CALLS
    "length" -> FinishReason.LENGTH
    "content_filter" -> FinishReason.CONTENT_FILTER
    else -> FinishReason.OTHER
}

private const val FUNCTION_TYPE = "function"
