package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import kotlinx.coroutines.flow.Flow

interface AssistantRuntime {
    fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    suspend fun cancelTurn(conversationId: String)

    suspend fun resolveConfirmation(result: ConfirmationResult): AssistantRuntimeConfirmationDispatchResult
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
        val confirmation: AssistantConfirmationCard,
    ) : AssistantRuntimeEvent

    data class ConfirmationResolved(
        val result: ConfirmationResult,
    ) : AssistantRuntimeEvent

    data class Finished(
        val outcome: LoopOutcome,
        val updatedHistory: List<AssistantMessage>,
        val retryAvailable: Boolean = false,
        val error: AssistantError? = null,
    ) : AssistantRuntimeEvent
}

sealed interface AssistantRuntimeConfirmationDispatchResult {
    data object Accepted : AssistantRuntimeConfirmationDispatchResult
    data object Deferred : AssistantRuntimeConfirmationDispatchResult
}
