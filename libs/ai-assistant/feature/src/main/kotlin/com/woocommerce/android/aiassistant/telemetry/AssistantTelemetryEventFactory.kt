package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantActionFamilyValue
import com.automattic.eventhorizon.AiAssistantCardFamilyValue
import com.automattic.eventhorizon.AiAssistantCardTappedEvent
import com.automattic.eventhorizon.AiAssistantConversationStartedEvent
import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantShowCardsProcessedEvent
import com.automattic.eventhorizon.AiAssistantToolCallCompletedEvent
import com.automattic.eventhorizon.AiAssistantToolStatusValue
import com.automattic.eventhorizon.AiAssistantTurnCompletedEvent
import com.automattic.eventhorizon.AiAssistantTurnOutcomeValue
import com.automattic.eventhorizon.AiAssistantTurnStartedEvent

object AssistantTelemetryEventFactory {
    fun conversationStarted(context: AssistantTelemetryContext) =
        AiAssistantConversationStartedEvent(
            conversationId = context.conversationId,
            requestId = context.requestId,
            messageId = context.messageId,
        )

    fun turnStarted(
        context: AssistantTelemetryContext,
        isRetry: Boolean,
        completionStack: String?,
        promptVersion: String?,
        toolCatalogVersion: String?,
    ) = AiAssistantTurnStartedEvent(
        conversationId = context.conversationId,
        requestId = context.requestId,
        messageId = context.messageId,
        isRetry = isRetry,
        completionStack = completionStack,
        promptVersion = promptVersion,
        toolCatalogVersion = toolCatalogVersion,
    )

    fun toolCallCompleted(
        context: AssistantTelemetryContext,
        toolName: String,
        status: AiAssistantToolStatusValue,
        errorKind: AiAssistantErrorKindValue?,
        durationMs: Long?,
    ) = AiAssistantToolCallCompletedEvent(
        conversationId = context.conversationId,
        requestId = context.requestId,
        messageId = context.messageId,
        toolName = toolName,
        status = status,
        errorKind = errorKind,
        durationMs = durationMs,
    )

    fun showCardsProcessed(
        context: AssistantTelemetryContext,
        requestedCount: Int,
        renderedCount: Int,
        missingCount: Int,
        rejectedCount: Int,
    ) = AiAssistantShowCardsProcessedEvent(
        conversationId = context.conversationId,
        requestId = context.requestId,
        messageId = context.messageId,
        requestedCount = requestedCount.toLong(),
        renderedCount = renderedCount.toLong(),
        missingCount = missingCount.toLong(),
        rejectedCount = rejectedCount.toLong(),
    )

    fun cardTapped(
        context: AssistantTelemetryContext,
        cardFamily: AiAssistantCardFamilyValue,
        actionFamily: AiAssistantActionFamilyValue,
    ) = AiAssistantCardTappedEvent(
        conversationId = context.conversationId,
        requestId = context.requestId,
        messageId = context.messageId,
        cardFamily = cardFamily,
        actionFamily = actionFamily,
    )

    @Suppress("LongParameterList")
    fun turnCompleted(
        context: AssistantTelemetryContext,
        outcome: AiAssistantTurnOutcomeValue,
        errorKind: AiAssistantErrorKindValue?,
        durationMs: Long,
        isRetry: Boolean,
        completionStack: String?,
        promptVersion: String?,
        toolCatalogVersion: String?,
    ) = AiAssistantTurnCompletedEvent(
        conversationId = context.conversationId,
        requestId = context.requestId,
        messageId = context.messageId,
        outcome = outcome,
        errorKind = errorKind,
        durationMs = durationMs,
        isRetry = isRetry,
        completionStack = completionStack,
        promptVersion = promptVersion,
        toolCatalogVersion = toolCatalogVersion,
    )
}
