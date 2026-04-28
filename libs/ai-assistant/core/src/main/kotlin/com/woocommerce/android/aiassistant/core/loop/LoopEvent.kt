package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult

sealed interface LoopEvent {
    data class AssistantTextDelta(val text: String) : LoopEvent
    data class ToolCallStarted(val call: ToolCall) : LoopEvent
    data class ToolCallFinished(val result: ToolResult) : LoopEvent
    data class AwaitingConfirmation(val call: ToolCall) : LoopEvent
    data class Finished(
        val outcome: LoopOutcome,
        val updatedHistory: List<AssistantMessage>,
        val retryAvailable: Boolean = false,
    ) : LoopEvent
}
