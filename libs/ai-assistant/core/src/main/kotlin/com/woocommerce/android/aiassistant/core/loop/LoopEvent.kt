package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult

sealed interface LoopEvent {
    data class AssistantTextDelta(val text: String) : LoopEvent
    data class ToolCallStarted(val call: ToolCall) : LoopEvent
    data class ToolCallFinished(
        val result: ToolResult,
        val toolName: String,
        val decision: ToolDecision,
        val durationMs: Long?,
    ) : LoopEvent
    data class ConfirmationRequested(val request: ConfirmationRequest) : LoopEvent
    data class ConfirmationResolved(val result: ConfirmationResult) : LoopEvent
    data class Failed(val error: AssistantError) : LoopEvent
    data class Finished(
        val outcome: LoopOutcome,
        val updatedHistory: List<AssistantMessage>,
        val retryAffordance: RetryAffordance = RetryAffordance.None,
        val error: AssistantError? = null,
    ) : LoopEvent
}
