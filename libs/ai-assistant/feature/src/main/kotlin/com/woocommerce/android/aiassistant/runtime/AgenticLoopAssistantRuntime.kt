package com.woocommerce.android.aiassistant.runtime

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantToolStatusValue
import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistoryMapper
import com.woocommerce.android.aiassistant.core.history.ModelRequestHistoryBuilder
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.loop.ToolDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewContext
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistry
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryContext
import com.woocommerce.android.aiassistant.telemetry.ShowCardsCounts
import com.woocommerce.android.aiassistant.telemetry.ShowCardsTelemetryReducer
import com.woocommerce.android.aiassistant.tools.handlers.cards.SHOW_CARDS_TOOL_NAME
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsStructured
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardUiStructuredParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject

internal class AgenticLoopAssistantRuntime @Inject constructor(
    private val agenticLoop: AgenticLoop,
    private val toolRegistry: ToolRegistry,
    private val toolCatalogSelector: ToolCatalogSelector,
    private val safetyOrchestrator: SafetyOrchestrator,
    private val confirmationPreviewProviderRegistry: ConfirmationPreviewProviderRegistry,
    private val confirmationPreviewRenderer: ConfirmationPreviewRenderer,
    private val cardParser: AssistantCardUiStructuredParser,
    private val systemPromptProvider: AssistantSystemPromptProvider,
    private val modelRequestHistoryBuilder: ModelRequestHistoryBuilder,
    private val sessionHistoryMapper: AssistantSessionHistoryMapper,
    @AiAssistantJson private val json: Json,
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

    @Suppress("LongMethod")
    private fun runTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> = flow {
        val context = SessionContext(
            siteId = request.siteId,
            catalogSnapshot = toolCatalogSelector.select(request.toolScope, toolRegistry.descriptors()),
        )
        var pendingError: AssistantError? = null
        val toolNamesById = mutableMapOf<String, String>()
        val modelHistory = modelRequestHistoryBuilder.build(
            systemPrompt = systemPromptProvider.systemPrompt(),
            sessionHistory = request.sessionHistory,
            currentUserMessage = request.userMessage,
        )

        agenticLoop.runTurn(
            conversationId = request.conversationId,
            modelHistory = modelHistory,
            context = context,
        ).collect { event ->
            when (event) {
                is LoopEvent.AssistantTextDelta -> emit(
                    AssistantRuntimeEvent.AssistantTextDelta(event.text)
                )
                is LoopEvent.ConfirmationRequested -> {
                    val descriptor = context.catalogSnapshot.tools
                        .firstOrNull { it.name == event.request.toolName }
                        ?: event.request.toFallbackDescriptor()
                    emit(
                        AssistantRuntimeEvent.AwaitingConfirmation(
                            event.request.toConfirmationCard(descriptor)
                        )
                    )
                }
                is LoopEvent.Finished -> {
                    val error = event.error ?: pendingError
                    val updatedSessionHistory = sessionHistoryMapper.appendTurn(
                        baseHistory = request.sessionHistory,
                        modelTurnMessages = event.modelTurnMessages,
                    )
                    emit(
                        AssistantRuntimeEvent.Finished(
                            outcome = event.outcome,
                            updatedSessionHistory = updatedSessionHistory,
                            retryAffordance = event.retryAffordance,
                            error = error,
                        )
                    )
                    pendingError = null
                }
                is LoopEvent.Failed -> {
                    toolNamesById.clear()
                    pendingError = event.error
                }
                is LoopEvent.ConfirmationResolved -> emit(
                    AssistantRuntimeEvent.ConfirmationResolved(event.result)
                )
                is LoopEvent.ToolCallStarted -> {
                    emit(event.toRuntimeEvent(toolNamesById))
                }
                is LoopEvent.ToolCallFinished -> {
                    val fallbackToolName = toolNamesById.remove(event.result.toolCallId)
                    event.toRuntimeEvents(request.telemetryContext, fallbackToolName).forEach { emit(it) }
                }
            }
        }
    }

    private fun LoopEvent.ToolCallStarted.toRuntimeEvent(
        toolNamesById: MutableMap<String, String>,
    ): AssistantRuntimeEvent.ToolCallStarted {
        toolNamesById[call.id] = call.name
        return AssistantRuntimeEvent.ToolCallStarted(
            toolCallId = call.id,
            toolName = call.name,
        )
    }

    private fun LoopEvent.ToolCallFinished.toRuntimeEvents(
        telemetryContext: AssistantTelemetryContext,
        fallbackToolName: String?,
    ): List<AssistantRuntimeEvent> = buildList {
        val safeToolName = toolName.ifEmpty { fallbackToolName.orEmpty() }
        val (status, errorKind) = toStatusAndErrorKind()
        add(
            AssistantRuntimeEvent.ToolCallFinished(
                toolCallId = result.toolCallId,
                toolName = safeToolName,
                status = status,
                errorKind = errorKind,
                durationMs = durationMs,
                emitTelemetry = decision != ToolDecision.REPLAYED,
                telemetryContext = telemetryContext,
            )
        )
        val cards = result.toCards(safeToolName)
        val showCardsCounts = result.toShowCardsCounts(safeToolName)
        if (showCardsCounts != null) {
            add(
                AssistantRuntimeEvent.ShowCardsProcessed(
                    counts = showCardsCounts,
                    telemetryContext = telemetryContext,
                )
            )
        }
        if (cards.isNotEmpty()) {
            add(AssistantRuntimeEvent.CardsResolved(cards))
        }
    }

    private fun LoopEvent.ToolCallFinished.toStatusAndErrorKind():
        Pair<AiAssistantToolStatusValue, AiAssistantErrorKindValue?> =
        when (decision) {
            ToolDecision.EXECUTED -> when (result) {
                is ToolResult.Success -> AiAssistantToolStatusValue.Success to null
                is ToolResult.ValidationError ->
                    AiAssistantToolStatusValue.Failure to AiAssistantErrorKindValue.ValidationError
                is ToolResult.RejectedBySafety ->
                    AiAssistantToolStatusValue.Failure to AiAssistantErrorKindValue.ValidationError
                is ToolResult.TransportError ->
                    AiAssistantToolStatusValue.Failure to AiAssistantErrorKindValue.ServerError
            }
            ToolDecision.MALFORMED_ARGUMENTS,
            ToolDecision.VALIDATION_FAILED,
            ToolDecision.REJECTED_BY_SAFETY,
            ToolDecision.CAP_EXCEEDED ->
                AiAssistantToolStatusValue.Failure to AiAssistantErrorKindValue.ValidationError
            ToolDecision.HANDLER_FAILED -> AiAssistantToolStatusValue.Failure to AiAssistantErrorKindValue.ServerError
            ToolDecision.REPLAYED -> AiAssistantToolStatusValue.Success to null
        }

    private fun ToolResult.toCards(toolName: String?): List<AssistantCard> = when {
        toolName == SHOW_CARDS_TOOL_NAME && this is ToolResult.Success ->
            cardParser.parse(uiStructured).map { it.card }
        else -> emptyList()
    }

    private fun ToolResult.toShowCardsCounts(toolName: String?): ShowCardsCounts? = when {
        toolName != SHOW_CARDS_TOOL_NAME || this !is ToolResult.Success -> null
        else -> runCatching {
            ShowCardsTelemetryReducer.reduce(json.decodeFromJsonElement<ShowCardsStructured>(structured))
        }.getOrNull()
    }

    private suspend fun ConfirmationRequest.toConfirmationCard(
        descriptor: ToolDescriptor,
    ): AssistantConfirmationCard {
        val preview = confirmationPreviewProviderRegistry.buildPreview(
            ConfirmationPreviewContext(
                request = this,
                descriptor = descriptor,
            )
        )
        return AssistantConfirmationCard(
            confirmationId = id,
            toolCall = ToolCall(
                id = toolCallId,
                name = toolName,
                arguments = arguments,
            ),
            state = AssistantConfirmationCardState.PENDING,
            preview = confirmationPreviewRenderer.render(preview),
        )
    }

    private fun ConfirmationRequest.toFallbackDescriptor(): ToolDescriptor = ToolDescriptor(
        name = toolName,
        description = "",
        inputSchema = buildJsonObject {},
        safetyLevel = safetyLevel,
    )
}
