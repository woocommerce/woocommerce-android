package com.woocommerce.android.aiassistant.runtime

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantToolStatusValue
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.RetryAffordance
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryContext
import com.woocommerce.android.aiassistant.telemetry.ShowCardsCounts
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import kotlinx.coroutines.flow.Flow

internal interface AssistantRuntime {
    fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent>

    suspend fun cancelTurn(conversationId: String)

    suspend fun resolveConfirmation(result: ConfirmationResult): AssistantRuntimeConfirmationDispatchResult
}

internal data class AssistantTurnRequest(
    val conversationId: String,
    val telemetryContext: AssistantTelemetryContext,
    val siteId: Long,
    val toolScope: ToolScope,
    val userMessage: String,
    val history: List<AssistantMessage>,
)

internal sealed interface AssistantRuntimeEvent {
    data class AssistantTextDelta(val text: String) : AssistantRuntimeEvent

    data class ToolCallStarted(
        val toolCallId: String,
        val toolName: String,
    ) : AssistantRuntimeEvent

    data class ToolCallFinished(
        val toolCallId: String,
        val toolName: String,
        val status: AiAssistantToolStatusValue,
        val errorKind: AiAssistantErrorKindValue?,
        val durationMs: Long?,
        val emitTelemetry: Boolean,
        val telemetryContext: AssistantTelemetryContext,
    ) : AssistantRuntimeEvent

    data class AwaitingConfirmation(
        val confirmation: AssistantConfirmationCard,
    ) : AssistantRuntimeEvent

    data class ConfirmationResolved(
        val result: ConfirmationResult,
    ) : AssistantRuntimeEvent

    data class CardsResolved(
        val cards: List<AssistantCard>,
    ) : AssistantRuntimeEvent

    data class ShowCardsProcessed(
        val counts: ShowCardsCounts,
        val telemetryContext: AssistantTelemetryContext,
    ) : AssistantRuntimeEvent

    data class Finished(
        val outcome: LoopOutcome,
        val updatedHistory: List<AssistantMessage>,
        val retryAffordance: RetryAffordance = RetryAffordance.None,
        val error: AssistantError? = null,
    ) : AssistantRuntimeEvent
}

internal sealed interface AssistantRuntimeConfirmationDispatchResult {
    data object Accepted : AssistantRuntimeConfirmationDispatchResult
    data object Deferred : AssistantRuntimeConfirmationDispatchResult
}
