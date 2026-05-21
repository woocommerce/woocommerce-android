package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistoryMapper
import com.woocommerce.android.aiassistant.core.history.ModelRequestHistoryBuilder
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.time.TimeSource

class WooAssistantHeadless(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val retryPolicy: RetryPolicy,
    private val historyBudgeter: HistoryBudgeter,
    private val json: Json,
    private val timeSource: TimeSource,
    private val safetyOrchestrator: SafetyOrchestrator = ScriptedHeadlessSafetyOrchestrator(),
) {
    suspend fun runScenario(scenario: HeadlessScenario): HeadlessRunResult {
        var sessionHistory = scenario.initialSessionHistory
        val modelRequestHistoryBuilder = ModelRequestHistoryBuilder(historyBudgeter)
        val sessionHistoryMapper = AssistantSessionHistoryMapper()
        val turns = scenario.turns.mapIndexed { index, turn ->
            val loop = AgenticLoopImpl(
                chatService = chatService,
                toolRegistry = toolRegistry,
                retryPolicy = retryPolicy,
                safetyOrchestrator = safetyOrchestrator,
                json = json,
                timeSource = timeSource,
            )
            val modelHistory = modelRequestHistoryBuilder.build(
                systemPrompt = scenario.systemPrompt,
                sessionHistory = sessionHistory,
                currentUserMessage = turn.userMessage,
            )
            val events = loop.runTurn(
                conversationId = scenario.id,
                modelHistory = modelHistory,
                context = scenario.context,
            ).toList()
            val finished = events.filterIsInstance<LoopEvent.Finished>().lastOrNull()
            if (finished != null) {
                sessionHistory = sessionHistoryMapper.appendTurn(
                    baseHistory = sessionHistory,
                    modelTurnMessages = finished.modelTurnMessages,
                    error = finished.error,
                )
            }
            HeadlessTurnResult(
                turnIndex = index,
                userMessage = turn.userMessage,
                assistantText = events.filterIsInstance<LoopEvent.AssistantTextDelta>().joinToString("") { it.text },
                outcome = finished?.outcome ?: LoopOutcome.FAILED,
                toolCalls = events.toToolCallTraces(scenario.context.catalogSnapshot.tools),
                confirmationRequests = events.filterIsInstance<LoopEvent.ConfirmationRequested>().map {
                    it.request.toTrace()
                },
                confirmationResults = events.filterIsInstance<LoopEvent.ConfirmationResolved>().map {
                    it.result.toTrace()
                },
                errors = events.toErrorTraces(finished),
            )
        }
        return HeadlessRunResult(scenarioId = scenario.id, turns = turns)
    }

    private fun List<LoopEvent>.toErrorTraces(finished: LoopEvent.Finished?): List<String> =
        (
            filterIsInstance<LoopEvent.Failed>().map { it.error.toTraceLabel() } +
                listOfNotNull(finished?.error?.toTraceLabel())
            ).distinct()

    private fun List<LoopEvent>.toToolCallTraces(descriptors: List<ToolDescriptor>): List<HeadlessToolCallTrace> {
        val startedCalls = mutableMapOf<String, ToolCall>()
        val confirmationRequests = mutableMapOf<String, ConfirmationRequest>()
        val traces = mutableListOf<HeadlessToolCallTrace>()
        forEach { event ->
            when (event) {
                is LoopEvent.ToolCallStarted -> startedCalls[event.call.id] = event.call
                is LoopEvent.ConfirmationRequested -> confirmationRequests[event.request.toolCallId] = event.request
                is LoopEvent.ToolCallFinished -> traces += event.toTrace(
                    startedCalls = startedCalls,
                    confirmationRequests = confirmationRequests,
                    descriptors = descriptors,
                )
                is LoopEvent.AssistantTextDelta,
                is LoopEvent.ConfirmationResolved,
                is LoopEvent.Failed,
                is LoopEvent.Finished -> Unit
            }
        }
        return traces
    }

    private fun LoopEvent.ToolCallFinished.toTrace(
        startedCalls: Map<String, ToolCall>,
        confirmationRequests: Map<String, ConfirmationRequest>,
        descriptors: List<ToolDescriptor>,
    ): HeadlessToolCallTrace {
        val call = startedCalls[result.toolCallId]
        val confirmationRequest = confirmationRequests[result.toolCallId]
        val safeToolName = call?.name ?: confirmationRequest?.toolName ?: toolName
        val descriptor = descriptors.firstOrNull { it.name == safeToolName }
        return HeadlessToolCallTrace(
            id = result.toolCallId,
            name = safeToolName,
            arguments = call?.arguments ?: confirmationRequest?.arguments ?: JsonObject(emptyMap()),
            safetyLevel = descriptor?.safetyLevel ?: confirmationRequest?.safetyLevel ?: ToolSafetyLevel.SAFE,
            resultKind = result.toResultKind(),
        )
    }

    private fun ToolResult.toResultKind(): HeadlessToolResultKind = when (this) {
        is ToolResult.Success -> HeadlessToolResultKind.SUCCESS
        is ToolResult.ValidationError -> HeadlessToolResultKind.VALIDATION_ERROR
        is ToolResult.TransportError -> HeadlessToolResultKind.TRANSPORT_ERROR
        is ToolResult.RejectedBySafety -> HeadlessToolResultKind.REJECTED_BY_SAFETY
    }

    private fun ConfirmationRequest.toTrace() = HeadlessConfirmationRequestTrace(
        id = id,
        toolCallId = toolCallId,
        toolName = toolName,
        arguments = arguments,
        safetyLevel = safetyLevel,
    )

    private fun ConfirmationResult.toTrace() = HeadlessConfirmationResultTrace(
        requestId = requestId,
        decision = decision.name,
    )

    private fun AssistantError.toTraceLabel(): String = when (this) {
        is AssistantError.Auth -> "AUTH"
        AssistantError.Cancelled -> "CANCELLED"
        is AssistantError.Network -> "NETWORK"
        is AssistantError.RateLimit -> "RATE_LIMIT"
        is AssistantError.BadRequest -> "BAD_REQUEST"
        is AssistantError.Timeout -> "TIMEOUT"
        is AssistantError.UpstreamFailure -> "UPSTREAM_FAILURE"
        is AssistantError.InvalidToolCall -> "INVALID_TOOL_CALL:$toolName"
        is AssistantError.OutcomeUnknown -> "OUTCOME_UNKNOWN:$toolName"
        is AssistantError.ToolFailed -> "TOOL_FAILED:$toolName"
        is AssistantError.Unknown -> "UNKNOWN"
    }
}
