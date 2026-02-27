package com.woocommerce.android.ui.ai.model

data class ChatMessage(
    val role: Role,
    val content: String?,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }
}

data class ToolCall(
    val id: String,
    val function: FunctionCall
)

data class FunctionCall(
    val name: String,
    val arguments: String
)
