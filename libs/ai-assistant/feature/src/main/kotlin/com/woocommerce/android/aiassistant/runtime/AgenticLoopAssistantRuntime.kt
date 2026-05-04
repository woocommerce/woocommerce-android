package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.ConfirmationSnapshot
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationSnapshotResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.SHOW_CARDS_TOOL_NAME
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardUiStructuredParser
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
    private val cardParser: AssistantCardUiStructuredParser,
) : AssistantRuntime {

    override fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override suspend fun cancelTurn(conversationId: String) = Unit

    override suspend fun resolveConfirmation(
        result: ConfirmationResult,
    ): AssistantRuntimeConfirmationDispatchResult =
        if (safetyOrchestrator.resolve(result)) {
            AssistantRuntimeConfirmationDispatchResult.Accepted
        } else {
            AssistantRuntimeConfirmationDispatchResult.Deferred
        }

    private fun runTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = flow {
        val context = SessionContext(
            siteId = request.siteId,
            catalogSnapshot = toolCatalogSelector.select(request.toolScope, toolRegistry.descriptors()),
        )
        var pendingError: AssistantError? = null
        val toolNamesById = mutableMapOf<String, String>()

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
                is LoopEvent.ToolCallStarted -> {
                    toolNamesById[event.call.id] = event.call.name
                    emit(
                        AssistantRuntimeEvent.ToolCallStarted(
                            toolCallId = event.call.id,
                            toolName = event.call.name,
                        )
                    )
                }
                is LoopEvent.ToolCallFinished -> {
                    val toolName = toolNamesById.remove(event.result.toolCallId)
                    emit(
                        AssistantRuntimeEvent.ToolCallFinished(
                            toolCallId = event.result.toolCallId,
                        )
                    )
                    val cards = event.result.toShowCards(toolName)
                    if (cards.isNotEmpty()) {
                        emit(AssistantRuntimeEvent.CardsResolved(cards))
                    }
                }
            }
        }
    }

    private fun ToolResult.toShowCards(toolName: String?) =
        if (toolName == SHOW_CARDS_TOOL_NAME && this is ToolResult.Success) {
            cardParser.parse(uiStructured)
        } else {
            emptyList()
        }

    private fun ConfirmationRequest.toConfirmationCard(snapshot: ConfirmationSnapshot?): AssistantConfirmationCard {
        return AssistantConfirmationCard(
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
}
