package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
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

class WooAssistantHeadless(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val retryPolicy: RetryPolicy,
    private val historyBudgeter: HistoryBudgeter,
    private val json: Json,
    private val safetyOrchestrator: SafetyOrchestrator = ScriptedHeadlessSafetyOrchestrator(),
) {
    suspend fun runScenario(scenario: HeadlessScenario): HeadlessRunResult {
        var history = scenario.initialHistory
        val turns = scenario.turns.mapIndexed { index, turn ->
            val loop = AgenticLoopImpl(
                chatService = chatService,
                toolRegistry = toolRegistry,
                retryPolicy = retryPolicy,
                historyBudgeter = historyBudgeter,
                safetyOrchestrator = safetyOrchestrator,
                json = json,
            )
            val events = loop.runTurn(
                conversationId = scenario.id,
                userMessage = turn.userMessage,
                history = history,
                context = scenario.context,
            ).toList()
            val finished = events.filterIsInstance<LoopEvent.Finished>().lastOrNull()
            if (finished != null) {
                history = finished.updatedHistory
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
                errors = events.filterIsInstance<LoopEvent.Failed>().map { it.error.toTraceLabel() },
            )
        }
        return HeadlessRunResult(scenarioId = scenario.id, turns = turns)
    }

    private fun List<LoopEvent>.toToolCallTraces(descriptors: List<ToolDescriptor>): List<HeadlessToolCallTrace> {
        val startedCalls = mutableMapOf<String, ToolCall>()
        val traces = mutableListOf<HeadlessToolCallTrace>()
        forEach { event ->
            when (event) {
                is LoopEvent.ToolCallStarted -> startedCalls[event.call.id] = event.call
                is LoopEvent.ToolCallFinished -> traces += event.result.toTrace(startedCalls, descriptors)
                is LoopEvent.AssistantTextDelta,
                is LoopEvent.ConfirmationRequested,
                is LoopEvent.ConfirmationResolved,
                is LoopEvent.Failed,
                is LoopEvent.Finished -> Unit
            }
        }
        return traces
    }

    private fun ToolResult.toTrace(
        startedCalls: Map<String, ToolCall>,
        descriptors: List<ToolDescriptor>,
    ): HeadlessToolCallTrace {
        val call = startedCalls[toolCallId]
        val descriptor = call?.let { c -> descriptors.firstOrNull { it.name == c.name } }
        return HeadlessToolCallTrace(
            id = toolCallId,
            name = call?.name.orEmpty(),
            arguments = call?.arguments ?: JsonObject(emptyMap()),
            safetyLevel = requireNotNull(descriptor) { "Missing descriptor for tool result: $toolCallId" }.safetyLevel,
            resultKind = toResultKind(),
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
        AssistantError.Auth -> "AUTH"
        AssistantError.Cancelled -> "CANCELLED"
        AssistantError.Network -> "NETWORK"
        AssistantError.RateLimit -> "RATE_LIMIT"
        AssistantError.BadRequest -> "BAD_REQUEST"
        AssistantError.Timeout -> "TIMEOUT"
        AssistantError.UpstreamFailure -> "UPSTREAM_FAILURE"
        is AssistantError.InvalidToolCall -> "INVALID_TOOL_CALL:$toolName"
        is AssistantError.OutcomeUnknown -> "OUTCOME_UNKNOWN:$toolName"
        is AssistantError.ToolFailed -> "TOOL_FAILED:$toolName"
        is AssistantError.Unknown -> "UNKNOWN"
    }
}
