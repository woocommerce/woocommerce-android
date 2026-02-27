package com.woocommerce.android.ui.ai.model

sealed class AIAssistantEvent {
    data class StreamingText(val chunk: String) : AIAssistantEvent()
    data class ToolCallStarted(val toolName: String) : AIAssistantEvent()
    data class ToolCallCompleted(val toolName: String, val result: String) : AIAssistantEvent()
    data class FinalResponse(val fullText: String, val toolCalls: List<ToolCall>? = null) : AIAssistantEvent()
    data class Error(val message: String) : AIAssistantEvent()
}
