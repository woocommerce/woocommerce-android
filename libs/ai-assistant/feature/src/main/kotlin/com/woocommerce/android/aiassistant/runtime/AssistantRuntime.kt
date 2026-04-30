package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import kotlinx.coroutines.flow.Flow

interface AssistantRuntime {
    fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    suspend fun cancelTurn(conversationId: String)

    suspend fun confirmWrite(confirmationId: String): AssistantRuntimeConfirmationResult

    suspend fun cancelWrite(confirmationId: String)
}

data class AssistantTurnRequest(
    val conversationId: String,
    val siteId: Long,
    val toolScope: ToolScope,
    val userMessage: String,
    val history: List<AssistantMessage>,
)

sealed interface AssistantRuntimeEvent {
    data class AssistantTextDelta(val text: String) : AssistantRuntimeEvent

    data class AwaitingConfirmation(
        val confirmation: AssistantPendingConfirmation,
    ) : AssistantRuntimeEvent

    data class Finished(
        val outcome: LoopOutcome,
        val updatedHistory: List<AssistantMessage>,
        val retryAvailable: Boolean = false,
        val error: AssistantError? = null,
    ) : AssistantRuntimeEvent
}

data class AssistantPendingConfirmation(
    val id: String,
    val toolCall: ToolCall,
)

sealed interface AssistantRuntimeConfirmationResult {
    data object Deferred : AssistantRuntimeConfirmationResult
}
