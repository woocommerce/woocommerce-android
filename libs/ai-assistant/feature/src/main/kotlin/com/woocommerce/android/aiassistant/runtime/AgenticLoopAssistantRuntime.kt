package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class AgenticLoopAssistantRuntime @Inject constructor(
    private val agenticLoop: AgenticLoop,
    private val toolRegistry: ToolRegistry,
    private val toolCatalogSelector: ToolCatalogSelector,
    private val safetyOrchestrator: SafetyOrchestrator,
    private val confirmationPreviewBuilder: WooCommerceConfirmationPreviewBuilder,
    private val confirmationPreviewRenderer: ConfirmationPreviewRenderer,
) : AssistantRuntime {

    override fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override suspend fun cancelTurn(conversationId: String) = Unit

    override suspend fun confirmWrite(confirmationId: String): AssistantRuntimeConfirmationResult =
        if (safetyOrchestrator.confirm(confirmationId)) {
            AssistantRuntimeConfirmationResult.Accepted
        } else {
            AssistantRuntimeConfirmationResult.Deferred
        }

    override suspend fun cancelWrite(confirmationId: String) {
        safetyOrchestrator.cancel(confirmationId)
    }

    private fun runTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = flow {
        val context = SessionContext(
            siteId = request.siteId,
            catalogSnapshot = toolCatalogSelector.select(request.toolScope, toolRegistry.descriptors()),
        )

        agenticLoop.runTurn(
            conversationId = request.conversationId,
            userMessage = request.userMessage,
            history = request.history,
            context = context,
        ).collect { event ->
            when (event) {
                is LoopEvent.AssistantTextDelta -> emit(
                    AssistantRuntimeEvent.AssistantTextDelta(event.text)
                )
                is LoopEvent.ConfirmationRequested -> emit(
                    AssistantRuntimeEvent.AwaitingConfirmation(event.request.toPendingConfirmation())
                )
                is LoopEvent.Finished -> emit(
                    AssistantRuntimeEvent.Finished(
                        outcome = event.outcome,
                        updatedHistory = event.updatedHistory,
                        retryAvailable = event.retryAvailable,
                        error = event.error,
                    )
                )
                is LoopEvent.ConfirmationResolved,
                is LoopEvent.Failed,
                is LoopEvent.ToolCallFinished,
                is LoopEvent.ToolCallStarted -> Unit
            }
        }
    }

    private fun ConfirmationRequest.toPendingConfirmation() = AssistantPendingConfirmation(
        id = id,
        toolCall = ToolCall(
            id = toolCallId,
            name = toolName,
            arguments = arguments,
        ),
        preview = confirmationPreviewRenderer.render(confirmationPreviewBuilder.build(this)),
    )
}
