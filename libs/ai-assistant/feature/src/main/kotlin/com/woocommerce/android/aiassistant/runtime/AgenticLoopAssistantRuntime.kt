package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AgenticLoopAssistantRuntime @Inject constructor(
    private val agenticLoop: AgenticLoop,
    private val toolRegistry: ToolRegistry,
    private val toolCatalogSelector: ToolCatalogSelector,
) : AssistantRuntime {

    override fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = runTurn(request)

    override suspend fun cancelTurn(conversationId: String) = Unit

    override suspend fun confirmWrite(confirmationId: String): AssistantRuntimeConfirmationResult =
        AssistantRuntimeConfirmationResult.Deferred

    override suspend fun cancelWrite(confirmationId: String) = Unit

    private fun runTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = flow {
        val context = SessionContext(
            siteId = request.siteId,
            catalogSnapshot = toolCatalogSelector.select(request.toolScope, toolRegistry.descriptors()),
        )

        try {
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
                    is LoopEvent.BlockedBySafety -> {
                        emit(
                            AssistantRuntimeEvent.AwaitingConfirmation(
                                AssistantPendingConfirmation(id = event.call.id, toolCall = event.call)
                            )
                        )
                        throw PendingConfirmationReached()
                    }
                    is LoopEvent.Finished -> emit(
                        AssistantRuntimeEvent.Finished(
                            updatedHistory = event.updatedHistory,
                            retryAvailable = event.retryAvailable,
                            error = event.error,
                        )
                    )
                    is LoopEvent.ToolCallFinished,
                    is LoopEvent.ToolCallStarted -> Unit
                }
            }
        } catch (_: PendingConfirmationReached) {
        }
    }

    private class PendingConfirmationReached : Throwable()
}
