package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall

/**
 * Opaque session history DTOs for the AI Assistant feature; not a supported public API.
 */
data class AssistantSessionHistory(
    val messages: List<AssistantSessionMessage> = emptyList(),
) {
    fun append(newMessages: List<AssistantSessionMessage>) = copy(messages = messages + newMessages)

    companion object {
        val Empty = AssistantSessionHistory()
    }
}

sealed interface AssistantSessionMessage {
    data class User(val content: String) : AssistantSessionMessage
    data class Assistant(val content: String) : AssistantSessionMessage
    data class ToolExchange(
        val assistantContent: String?,
        val toolCalls: List<ToolCall>,
        val toolResults: List<AssistantMessage.Tool>,
    ) : AssistantSessionMessage {
        init {
            require(toolCalls.isNotEmpty()) { "ToolExchange requires at least one tool call" }
            require(toolCalls.map(ToolCall::id).distinct().size == toolCalls.size) {
                "ToolExchange tool call ids must be unique"
            }
            require(toolResults.map(AssistantMessage.Tool::toolCallId) == toolCalls.map(ToolCall::id)) {
                "ToolExchange tool results must match tool calls in order"
            }
        }
    }
}
