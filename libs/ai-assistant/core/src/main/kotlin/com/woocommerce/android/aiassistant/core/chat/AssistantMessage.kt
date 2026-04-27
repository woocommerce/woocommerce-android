package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonObject

/**
 * A single message in the conversation passed to [ChatService]. The runtime
 * keeps these as a pure-Kotlin sealed hierarchy so the loop can append turns
 * without depending on transport-level JSON shapes.
 */
sealed interface AssistantMessage {
    val role: String

    data class System(val content: String) : AssistantMessage {
        override val role: String = "system"
    }

    data class User(val content: String) : AssistantMessage {
        override val role: String = "user"
    }

    data class Assistant(
        val content: String?,
        val toolCalls: List<ToolCall> = emptyList(),
    ) : AssistantMessage {
        override val role: String = "assistant"
    }

    data class Tool(
        val toolCallId: String,
        val content: String,
    ) : AssistantMessage {
        override val role: String = "tool"
    }
}

/**
 * One assembled tool call returned by the model. The agentic loop concatenates
 * streamed [AssistantEvent.ToolCallDelta] fragments into this shape before
 * dispatching to the registry.
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
)
