package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolDiagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolFailureSource
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.toAssistantError
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestratorImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class AgenticLoopImpl(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val retryPolicy: RetryPolicy,
    private val historyBudgeter: HistoryBudgeter,
    private val safetyOrchestrator: SafetyOrchestrator = SafetyOrchestratorImpl(),
    private val json: Json,
) : AgenticLoop {

    @Suppress("LongMethod")
    override fun runTurn(
        conversationId: String,
        userMessage: String,
        history: List<AssistantMessage>,
        context: SessionContext,
    ): Flow<LoopEvent> = flow {
        val initialTurn = buildInitialTurn(userMessage, history)
        val assembler = ToolCallAssembler(json)
        val toolDescriptors = context.catalogSnapshot.tools
        val toolDefs = toolDescriptors.map { it.toToolDefinition() }
        val newTurnMessages = mutableListOf<AssistantMessage>(initialTurn.currentUserTurn)
        var modelMessages: List<AssistantMessage> = initialTurn.modelMessages
        var visibleOutputStarted = false
        val replayTracker = ToolReplayTracker(json)
        var iteration = 0
        var pendingInvalidToolCallError: AssistantError.InvalidToolCall? = null

        while (iteration < MAX_ITERATIONS) {
            val stream = streamWithRetry(
                ChatRequest(messages = modelMessages, tools = toolDefs),
                history + newTurnMessages,
                visibleOutputStarted,
            ) ?: return@flow
            visibleOutputStarted = stream.visibleOutputStarted

            val assembledToolCalls = assembler.assembleToolCalls(stream)

            val newAssistantMsg = AssistantMessage.Assistant(
                content = stream.assistantText.takeIf { it.isNotEmpty() },
                toolCalls = assembledToolCalls.callsForHistory,
            )

            if (stream.finishReason == null) {
                newTurnMessages.add(newAssistantMsg)
                emit(failedFinish(history + newTurnMessages, RetryAffordance.None, AssistantError.UpstreamFailure()))
                return@flow
            }

            val isTerminal = stream.finishReason == FinishReason.STOP ||
                (stream.finishReason != FinishReason.TOOL_CALLS && stream.toolCallDeltas.isEmpty())
            if (isTerminal) {
                modelMessages = modelMessages + newAssistantMsg
                newTurnMessages.add(newAssistantMsg)
                emit(LoopEvent.Finished(LoopOutcome.COMPLETED, history + newTurnMessages))
                return@flow
            }

            val toolExecution = executeTools(
                assembledToolCalls.results,
                assembledToolCalls.validCalls,
                toolDescriptors,
                replayTracker,
            )
            if (toolExecution is ToolExecutionOutcome.Cancelled) {
                emitCancelledToolExecution(toolExecution, stream.assistantText, history, newTurnMessages)
                return@flow
            }

            val completedTools = (toolExecution as ToolExecutionOutcome.Completed).completed
            pendingInvalidToolCallError = completedTools.terminalInvalidToolCallError()
            modelMessages = modelMessages + newAssistantMsg
            newTurnMessages.add(newAssistantMsg)
            modelMessages = appendCompletedToolMessages(modelMessages, newTurnMessages, completedTools)
            completedTools.firstUnsafeTransportFailureError()?.let { error ->
                emit(LoopEvent.Failed(error))
                emit(failedFinish(history + newTurnMessages, RetryAffordance.None, error))
                return@flow
            }
            iteration++
        }

        emitMaxIterationsOrInvalidToolCallFailure(
            pendingInvalidToolCallError,
            history + newTurnMessages,
        )
    }

    private fun buildInitialTurn(
        userMessage: String,
        history: List<AssistantMessage>,
    ): InitialTurnState {
        val currentUserTurn = AssistantMessage.User(userMessage)
        val systemPrompt = history.filterIsInstance<AssistantMessage.System>().firstOrNull()
            ?: AssistantMessage.System("")
        val rawTranscript = history.filterNot { it is AssistantMessage.System }
        val budgeted = historyBudgeter.build(systemPrompt, rawTranscript, currentUserTurn)

        return InitialTurnState(currentUserTurn, budgeted.messages)
    }

    private suspend fun FlowCollector<LoopEvent>.streamWithRetry(
        request: ChatRequest,
        fullHistory: List<AssistantMessage>,
        initialVisibleOutput: Boolean,
    ): StreamResult? {
        var visibleOutputStarted = initialVisibleOutput
        var retryCount = 0

        while (true) {
            val toolCallDeltas = mutableListOf<AssistantEvent.ToolCallDelta>()
            val assistantText = StringBuilder()
            var finishReason: FinishReason? = null
            var streamFailed: AssistantEvent.Failed? = null

            chatService.streamTurn(request).collect { event ->
                when (event) {
                    is AssistantEvent.TextDelta -> {
                        visibleOutputStarted = true
                        assistantText.append(event.text)
                        emit(LoopEvent.AssistantTextDelta(event.text))
                    }
                    is AssistantEvent.ToolCallDelta -> toolCallDeltas += event
                    is AssistantEvent.Finish -> finishReason = event.reason
                    is AssistantEvent.Failed -> streamFailed = event
                }
            }

            val failure = streamFailed ?: return StreamResult(
                toolCallDeltas = toolCallDeltas.toList(),
                assistantText = assistantText.toString(),
                finishReason = finishReason,
                visibleOutputStarted = visibleOutputStarted,
            )

            val widenedError = failure.kind.toAssistantError(failure.cause, failure.diagnostics)
            if (widenedError == AssistantError.Cancelled) {
                emitStoppedCancellation(fullHistory, assistantText.toString())
                return null
            }
            when (
                val decision = retryPolicy.decide(
                    LoopFailureContext(widenedError, visibleOutputStarted, retryCount)
                )
            ) {
                is RetryDecision.RetryNow -> {
                    delay(decision.backoffMs)
                    retryCount++
                }
                is RetryDecision.ShowManualRetry -> {
                    val failedHistory = messagesWithPartialText(fullHistory, assistantText.toString())
                    emit(failedFinish(failedHistory, RetryAffordance.Manual, widenedError))
                    return null
                }
                is RetryDecision.DoNotRetry -> {
                    val failedHistory = messagesWithPartialText(fullHistory, assistantText.toString())
                    emit(failedFinish(failedHistory, RetryAffordance.None, widenedError))
                    return null
                }
            }
        }
    }

    private suspend fun FlowCollector<LoopEvent>.emitStoppedCancellation(
        fullHistory: List<AssistantMessage>,
        assistantText: String,
    ) {
        emit(LoopEvent.Failed(AssistantError.Cancelled))
        emit(
            LoopEvent.Finished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = messagesWithPartialText(fullHistory, assistantText),
                retryAffordance = RetryAffordance.None,
                error = AssistantError.Cancelled,
            )
        )
    }

    private suspend fun FlowCollector<LoopEvent>.emitMaxIterationsOrInvalidToolCallFailure(
        invalidToolCallError: AssistantError.InvalidToolCall?,
        updatedHistory: List<AssistantMessage>,
    ) {
        invalidToolCallError?.let { error ->
            emit(LoopEvent.Failed(error))
            emit(failedFinish(updatedHistory, RetryAffordance.None, error))
            return
        }
        emit(LoopEvent.Finished(LoopOutcome.MAX_ITERATIONS, updatedHistory))
    }

    private suspend fun FlowCollector<LoopEvent>.executeTools(
        assembledResults: List<ToolCallAssembler.AssemblyResult>,
        validCalls: List<ToolCall>,
        toolDescriptors: List<ToolDescriptor>,
        replayTracker: ToolReplayTracker,
    ): ToolExecutionOutcome {
        val completedTools = mutableListOf<CompletedToolCall>()
        for (r in assembledResults) {
            if (r is ToolCallAssembler.AssemblyResult.MalformedArguments) {
                val result = ToolResult.ValidationError(r.callId, "Malformed arguments for ${r.toolName}")
                completedTools += CompletedToolCall(
                    historyToolCall = r.toHistoryToolCall(),
                    result = result,
                    safetyLevel = ToolSafetyLevel.SAFE,
                    invalidToolCallError = invalidToolCallError(r.toolName),
                )
                emit(LoopEvent.ToolCallFinished(result))
            }
        }
        for (call in validCalls) {
            val descriptor = toolDescriptors.find { it.name == call.name }
            when (val replayDecision = replayTracker.prepare(call)) {
                is ToolReplayDecision.CapExceeded -> {
                    completedTools += CompletedToolCall(
                        call,
                        replayDecision.result,
                        descriptor?.safetyLevel ?: ToolSafetyLevel.SAFE,
                        invalidToolCallError = descriptor.invalidToolCallErrorFor(call),
                    )
                    emit(LoopEvent.ToolCallFinished(replayDecision.result))
                }
                is ToolReplayDecision.Replay -> {
                    completedTools += CompletedToolCall(
                        call,
                        replayDecision.result,
                        descriptor?.safetyLevel ?: ToolSafetyLevel.SAFE,
                        invalidToolCallError = descriptor.invalidToolCallErrorFor(call),
                    )
                    emit(LoopEvent.ToolCallFinished(replayDecision.result))
                }
                is ToolReplayDecision.Execute -> {
                    val result = executeToolIfAllowed(call, descriptor)
                        ?: return ToolExecutionOutcome.Cancelled(completedTools)
                    replayTracker.record(replayDecision.signature, result)
                    completedTools += CompletedToolCall(
                        historyToolCall = call,
                        result = result,
                        safetyLevel = descriptor?.safetyLevel ?: ToolSafetyLevel.SAFE,
                        invalidToolCallError = descriptor.invalidToolCallErrorFor(call),
                    )
                    emit(LoopEvent.ToolCallFinished(result))
                }
            }
        }
        return ToolExecutionOutcome.Completed(completedTools)
    }

    private suspend fun FlowCollector<LoopEvent>.executeToolIfAllowed(
        call: ToolCall,
        descriptor: ToolDescriptor?,
    ): ToolResult? {
        if (descriptor == null) {
            return ToolResult.ValidationError(call.id, "Unknown tool: ${call.name}")
        }

        return when (val decision = safetyOrchestrator.evaluate(call, descriptor)) {
            SafetyDecision.Execute -> executeApprovedTool(call)
            is SafetyDecision.RequireConfirmation -> executeAfterConfirmation(call, decision)
        }
    }

    private suspend fun FlowCollector<LoopEvent>.executeAfterConfirmation(
        call: ToolCall,
        decision: SafetyDecision.RequireConfirmation,
    ): ToolResult? {
        val requestId = decision.request.id
        return try {
            emit(LoopEvent.ConfirmationRequested(decision.request))
            val confirmationResult = safetyOrchestrator.awaitResult(requestId)
            emit(LoopEvent.ConfirmationResolved(confirmationResult))
            when (confirmationResult.decision) {
                ConfirmationDecision.CONFIRMED -> executeApprovedTool(call)
                ConfirmationDecision.CANCELLED -> null
            }
        } finally {
            safetyOrchestrator.cancelPending(requestId)
        }
    }

    private suspend fun FlowCollector<LoopEvent>.executeApprovedTool(call: ToolCall): ToolResult {
        emit(LoopEvent.ToolCallStarted(call))
        return toolRegistry.execute(call)
    }

    private suspend fun FlowCollector<LoopEvent>.emitCancelledToolExecution(
        outcome: ToolExecutionOutcome.Cancelled,
        assistantText: String,
        history: List<AssistantMessage>,
        newTurnMessages: MutableList<AssistantMessage>,
    ) {
        val completedCalls = outcome.completed.map { it.historyToolCall }
        val cancelledAssistantMessage = AssistantMessage.Assistant(
            content = assistantText.takeIf { it.isNotEmpty() },
            toolCalls = completedCalls,
        )
        if (cancelledAssistantMessage.content != null || completedCalls.isNotEmpty()) {
            newTurnMessages.add(cancelledAssistantMessage)
        }
        outcome.completed.forEach { completed ->
            newTurnMessages.add(completed.toToolMessage())
        }
        emit(LoopEvent.Finished(LoopOutcome.STOPPED, history + newTurnMessages, RetryAffordance.None))
    }

    private fun appendCompletedToolMessages(
        modelMessages: List<AssistantMessage>,
        newTurnMessages: MutableList<AssistantMessage>,
        completedTools: List<CompletedToolCall>,
    ): List<AssistantMessage> {
        var updatedModelMessages = modelMessages
        completedTools.forEach { completed ->
            val newToolMsg = completed.toToolMessage()
            updatedModelMessages = updatedModelMessages + newToolMsg
            newTurnMessages.add(newToolMsg)
        }
        return updatedModelMessages
    }

    private data class StreamResult(
        val toolCallDeltas: List<AssistantEvent.ToolCallDelta>,
        val assistantText: String,
        val finishReason: FinishReason?,
        val visibleOutputStarted: Boolean,
    )

    private data class InitialTurnState(
        val currentUserTurn: AssistantMessage.User,
        val modelMessages: List<AssistantMessage>,
    )

    private data class AssembledToolCalls(
        val results: List<ToolCallAssembler.AssemblyResult>,
        val callsForHistory: List<ToolCall>,
        val validCalls: List<ToolCall>,
    )

    private fun ToolCallAssembler.assembleToolCalls(stream: StreamResult): AssembledToolCalls {
        val results = assemble(stream.toolCallDeltas)
        return AssembledToolCalls(
            results = results,
            callsForHistory = results.map { it.toHistoryToolCall() },
            validCalls = results
                .filterIsInstance<ToolCallAssembler.AssemblyResult.Success>()
                .map { it.call },
        )
    }

    private sealed interface ToolExecutionOutcome {
        data class Completed(val completed: List<CompletedToolCall>) : ToolExecutionOutcome
        data class Cancelled(val completed: List<CompletedToolCall>) : ToolExecutionOutcome
    }

    private data class CompletedToolCall(
        val historyToolCall: ToolCall,
        val result: ToolResult,
        val safetyLevel: ToolSafetyLevel,
        val invalidToolCallError: AssistantError.InvalidToolCall? = null,
    )

    private fun List<CompletedToolCall>.terminalInvalidToolCallError(): AssistantError.InvalidToolCall? {
        if (isEmpty() || any { completed -> completed.result !is ToolResult.ValidationError }) {
            return null
        }
        val invalidErrors = mapNotNull(CompletedToolCall::invalidToolCallError)
        return invalidErrors.firstOrNull().takeIf { invalidErrors.size == size }
    }

    private fun ToolDescriptor?.invalidToolCallErrorFor(call: ToolCall): AssistantError.InvalidToolCall? =
        if (this == null) invalidToolCallError(call.name) else null

    private fun invalidToolCallError(toolName: String) = AssistantError.InvalidToolCall(
        toolName = toolName,
        diagnostics = Diagnostics(
            tool = ToolDiagnostics(
                toolName = toolName,
                retryable = false,
                source = ToolFailureSource.INVALID_TOOL_CALL,
            )
        )
    )

    private fun List<CompletedToolCall>.firstUnsafeTransportFailureError(): AssistantError? =
        firstNotNullOfOrNull { completed ->
            val result = completed.result as? ToolResult.TransportError
                ?: return@firstNotNullOfOrNull null
            if (completed.safetyLevel != ToolSafetyLevel.UNSAFE) {
                return@firstNotNullOfOrNull null
            }
            val diagnostics = result.diagnostics.withToolDiagnostics(
                toolName = completed.historyToolCall.name,
                failureKind = result.kind,
                retryable = result.retryable,
                source = result.diagnostics.tool?.source ?: ToolFailureSource.TOOL_RESULT,
            )

            when (result.kind) {
                ToolFailureKind.OUTCOME_UNKNOWN -> AssistantError.OutcomeUnknown(
                    toolName = completed.historyToolCall.name,
                    diagnostics = diagnostics,
                )
                ToolFailureKind.DETERMINISTIC_FAILURE -> AssistantError.ToolFailed(
                    toolName = completed.historyToolCall.name,
                    diagnostics = diagnostics,
                )
            }
        }

    private fun Diagnostics.withToolDiagnostics(
        toolName: String,
        failureKind: ToolFailureKind,
        retryable: Boolean,
        source: ToolFailureSource,
    ) = copy(
        tool = ToolDiagnostics(
            toolName = toolName,
            failureKind = failureKind,
            retryable = retryable,
            source = source,
        )
    )

    private fun messagesWithPartialText(
        messages: List<AssistantMessage>,
        partial: String,
    ): List<AssistantMessage> = if (partial.isNotEmpty()) {
        messages + AssistantMessage.Assistant(content = partial, toolCalls = emptyList())
    } else {
        messages
    }

    private fun ToolResult.toModelContent(): String = when (this) {
        is ToolResult.Success -> structured.toString()
        is ToolResult.ValidationError -> errorJson(reason)
        is ToolResult.RejectedBySafety -> errorJson("Action was not approved")
        is ToolResult.TransportError -> errorJson("Tool execution failed")
    }

    private fun CompletedToolCall.toToolMessage() = AssistantMessage.Tool(
        toolCallId = result.toolCallId,
        content = result.toModelContent(),
    )

    private fun ToolCallAssembler.AssemblyResult.toHistoryToolCall(): ToolCall = when (this) {
        is ToolCallAssembler.AssemblyResult.Success -> call
        is ToolCallAssembler.AssemblyResult.MalformedArguments -> ToolCall(
            id = callId,
            name = toolName,
            arguments = JsonObject(emptyMap()),
        )
    }

    private fun errorJson(message: String): String = json.encodeToString(
        JsonObject.serializer(),
        JsonObject(mapOf("error" to JsonPrimitive(message))),
    )

    private fun ToolDescriptor.toToolDefinition() = ToolDefinition(
        name = name,
        description = description,
        parameters = inputSchema,
    )

    private fun failedFinish(
        history: List<AssistantMessage>,
        retryAffordance: RetryAffordance,
        error: AssistantError,
    ) = LoopEvent.Finished(LoopOutcome.FAILED, history, retryAffordance = retryAffordance, error = error)

    companion object {
        internal const val MAX_ITERATIONS = 5
    }
}
