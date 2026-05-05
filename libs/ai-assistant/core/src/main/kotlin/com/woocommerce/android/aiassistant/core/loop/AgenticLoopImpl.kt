package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AgenticLoopImpl(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val retryPolicy: RetryPolicy,
    private val historyBudgeter: HistoryBudgeter,
    private val safetyOrchestrator: SafetyOrchestrator = SafetyOrchestratorImpl(),
    private val json: Json,
) : AgenticLoop {

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

        while (iteration < MAX_ITERATIONS) {
            val stream = streamWithRetry(
                ChatRequest(messages = modelMessages, tools = toolDefs),
                history + newTurnMessages,
                visibleOutputStarted,
            ) ?: return@flow
            visibleOutputStarted = stream.visibleOutputStarted

            val assembledResults = assembler.assemble(stream.toolCallDeltas)
            val callsForHistory = assembledResults.map { it.toHistoryToolCall() }
            val validCalls = assembledResults
                .filterIsInstance<ToolCallAssembler.AssemblyResult.Success>()
                .map { it.call }

            val newAssistantMsg = AssistantMessage.Assistant(
                content = stream.assistantText.takeIf { it.isNotEmpty() },
                toolCalls = callsForHistory,
            )

            if (stream.finishReason == null) {
                newTurnMessages.add(newAssistantMsg)
                emit(failedFinish(history + newTurnMessages, retryAvailable = false, AssistantError.UpstreamFailure))
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

            val toolExecution = executeTools(assembledResults, validCalls, toolDescriptors, replayTracker)
            if (toolExecution is ToolExecutionOutcome.Cancelled) {
                emitCancelledToolExecution(toolExecution, stream.assistantText, history, newTurnMessages)
                return@flow
            }

            val completedTools = (toolExecution as ToolExecutionOutcome.Completed).completed
            modelMessages = modelMessages + newAssistantMsg
            newTurnMessages.add(newAssistantMsg)
            modelMessages = appendCompletedToolMessages(modelMessages, newTurnMessages, completedTools)
            completedTools.firstOutcomeUnknownError()?.let { error ->
                emit(LoopEvent.Failed(error))
                emit(failedFinish(history + newTurnMessages, retryAvailable = false, error))
                return@flow
            }
            iteration++
        }

        emit(LoopEvent.Finished(LoopOutcome.MAX_ITERATIONS, history + newTurnMessages))
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

            val widenedError = failure.kind.toAssistantError(failure.cause)
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
                    emit(failedFinish(failedHistory, retryAvailable = true, widenedError))
                    return null
                }
                is RetryDecision.DoNotRetry -> {
                    val failedHistory = messagesWithPartialText(fullHistory, assistantText.toString())
                    emit(failedFinish(failedHistory, retryAvailable = false, widenedError))
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
                retryAvailable = false,
                error = AssistantError.Cancelled,
            )
        )
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
                completedTools += CompletedToolCall(r.toHistoryToolCall(), result, ToolSafetyLevel.SAFE)
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
                    )
                    emit(LoopEvent.ToolCallFinished(replayDecision.result))
                }
                is ToolReplayDecision.Replay -> {
                    completedTools += CompletedToolCall(
                        call,
                        replayDecision.result,
                        descriptor?.safetyLevel ?: ToolSafetyLevel.SAFE,
                    )
                    emit(LoopEvent.ToolCallFinished(replayDecision.result))
                }
                is ToolReplayDecision.Execute -> {
                    val result = executeToolIfAllowed(call, descriptor)
                        ?: return ToolExecutionOutcome.Cancelled(completedTools)
                    replayTracker.record(replayDecision.signature, result)
                    completedTools += CompletedToolCall(call, result, descriptor?.safetyLevel ?: ToolSafetyLevel.SAFE)
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
        emit(LoopEvent.Finished(LoopOutcome.STOPPED, history + newTurnMessages, retryAvailable = false))
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

    private sealed interface ToolExecutionOutcome {
        data class Completed(val completed: List<CompletedToolCall>) : ToolExecutionOutcome
        data class Cancelled(val completed: List<CompletedToolCall>) : ToolExecutionOutcome
    }

    private inner class ToolReplayTracker(private val json: Json) {
        private val cachedSuccessesBySignature = mutableMapOf<String, ToolResult.Success>()
        private val callCountsBySignature = mutableMapOf<String, Int>()
        private val callCountsByToolName = mutableMapOf<String, Int>()

        fun prepare(call: ToolCall): ToolReplayDecision {
            val toolCallCount = callCountsByToolName.increment(call.name)
            if (toolCallCount > PER_TOOL_TURN_CALL_LIMIT) {
                return ToolReplayDecision.CapExceeded(
                    ToolResult.ValidationError(
                        toolCallId = call.id,
                        reason = "$PER_TOOL_CAP_REASON_PREFIX ${call.name}. $PER_TOOL_CAP_REASON_SUFFIX",
                    )
                )
            }

            val signature = call.canonicalSignature(json)
            val signatureCount = callCountsBySignature.increment(signature)
            val cached = cachedSuccessesBySignature[signature]

            return if (cached == null || signatureCount == 1) {
                ToolReplayDecision.Execute(signature)
            } else {
                ToolReplayDecision.Replay(
                    cached.toReplayResult(
                        toolCallId = call.id,
                        hint = if (signatureCount == SECOND_IDENTICAL_CALL_COUNT) {
                            DUPLICATE_REPLAY_SOFT_HINT
                        } else {
                            DUPLICATE_REPLAY_ESCALATED_HINT
                        }
                    )
                )
            }
        }

        fun record(signature: String, result: ToolResult) {
            if (result is ToolResult.Success) {
                cachedSuccessesBySignature.putIfAbsent(signature, result)
            }
        }

        private fun MutableMap<String, Int>.increment(key: String): Int {
            val updated = getOrDefault(key, 0) + 1
            this[key] = updated
            return updated
        }
    }

    private sealed interface ToolReplayDecision {
        data class Execute(val signature: String) : ToolReplayDecision
        data class Replay(val result: ToolResult.Success) : ToolReplayDecision
        data class CapExceeded(val result: ToolResult.ValidationError) : ToolReplayDecision
    }

    private data class CompletedToolCall(
        val historyToolCall: ToolCall,
        val result: ToolResult,
        val safetyLevel: ToolSafetyLevel,
    )

    private fun List<CompletedToolCall>.firstOutcomeUnknownError(): AssistantError.OutcomeUnknown? =
        firstNotNullOfOrNull { completed ->
            val isUnknownWriteOutcome = completed.safetyLevel == ToolSafetyLevel.UNSAFE &&
                completed.result is ToolResult.TransportError
            if (isUnknownWriteOutcome) {
                AssistantError.OutcomeUnknown(toolName = completed.historyToolCall.name)
            } else {
                null
            }
        }

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

    private fun ToolCall.canonicalSignature(json: Json): String =
        "$name|${json.encodeToString(JsonElement.serializer(), arguments.toCanonicalJsonElement())}"

    private fun JsonElement.toCanonicalJsonElement(): JsonElement = when (this) {
        is JsonObject -> JsonObject(
            entries
                .sortedBy { it.key }
                .associate { (key, value) -> key to value.toCanonicalJsonElement() }
        )
        is JsonArray -> JsonArray(map { it.toCanonicalJsonElement() })
        else -> this
    }

    private fun ToolResult.Success.toReplayResult(
        toolCallId: String,
        hint: String,
    ): ToolResult.Success = copy(
        toolCallId = toolCallId,
        structured = structured.withReplayHint(hint),
    )

    private fun JsonElement.withReplayHint(hint: String): JsonObject = when (this) {
        is JsonObject -> JsonObject(this + (DUPLICATE_REPLAY_HINT_FIELD to JsonPrimitive(hint)))
        else -> buildJsonObject {
            put(DUPLICATE_REPLAY_RESULT_FIELD, this@withReplayHint)
            put(DUPLICATE_REPLAY_HINT_FIELD, hint)
        }
    }

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
        retryAvailable: Boolean,
        error: AssistantError,
    ) = LoopEvent.Finished(LoopOutcome.FAILED, history, retryAvailable = retryAvailable, error = error)

    companion object {
        internal const val MAX_ITERATIONS = 5
        private const val SECOND_IDENTICAL_CALL_COUNT = 2
        private const val PER_TOOL_TURN_CALL_LIMIT = 4
        private const val DUPLICATE_REPLAY_HINT_FIELD = "_assistant_runtime_hint"
        private const val DUPLICATE_REPLAY_RESULT_FIELD = "result"
        private const val DUPLICATE_REPLAY_SOFT_HINT = "You already fetched this - use the result above."
        private const val DUPLICATE_REPLAY_ESCALATED_HINT =
            "STOP - you have called this tool identically 3+ times. Use the result above and finish now."
        private const val PER_TOOL_CAP_REASON_PREFIX = "Tool call limit exceeded for"
        private const val PER_TOOL_CAP_REASON_SUFFIX =
            "This tool was already called 4 times this turn. Use the results above and finish now."
    }
}
