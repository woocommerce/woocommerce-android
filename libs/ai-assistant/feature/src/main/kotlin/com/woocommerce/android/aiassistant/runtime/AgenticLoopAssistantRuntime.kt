package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationSnapshot
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationSnapshotResolver
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
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
    private val confirmationSnapshotResolver: WooCommerceConfirmationSnapshotResolver,
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
        var pendingError: AssistantError? = null

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
                is LoopEvent.ConfirmationRequested -> {
                    val snapshot = confirmationSnapshotResolver.resolve(event.request)
                    emit(
                        AssistantRuntimeEvent.AwaitingConfirmation(
                            event.request.toConfirmationCard(snapshot)
                        )
                    )
                }
                is LoopEvent.Finished -> {
                    emit(
                        AssistantRuntimeEvent.Finished(
                            outcome = event.outcome,
                            updatedHistory = event.updatedHistory,
                            retryAvailable = event.retryAvailable,
                            error = event.error ?: pendingError,
                        )
                    )
                    pendingError = null
                }
                is LoopEvent.Failed -> pendingError = event.error
                is LoopEvent.ConfirmationResolved -> emit(
                    AssistantRuntimeEvent.ConfirmationResolved(event.result)
                )
                is LoopEvent.ToolCallFinished,
                is LoopEvent.ToolCallStarted -> Unit
            }
        }
    }

    private fun ConfirmationRequest.toConfirmationCard(snapshot: ConfirmationSnapshot?) =
        AssistantConfirmationCard(
        confirmationId = id,
        toolCall = ToolCall(
            id = toolCallId,
            name = toolName,
            arguments = arguments,
        ),
        state = AssistantConfirmationCardState.PENDING,
        preview = confirmationPreviewRenderer.render(confirmationPreviewBuilder.build(this, snapshot)),
    )
}
