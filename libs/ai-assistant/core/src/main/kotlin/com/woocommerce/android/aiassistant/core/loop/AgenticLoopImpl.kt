package com.woocommerce.android.aiassistant.core.loop

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
    private val json: Json,
) : AgenticLoop {

    override fun runTurn(
        conversationId: String,
        userMessage: String,
        history: List<AssistantMessage>,
        context: SessionContext,
    ): Flow<LoopEvent> = flow {
        val toolDescriptors = context.catalogSnapshot.tools
        val toolDefs = toolDescriptors.map { it.toToolDefinition() }
        val assembler = ToolCallAssembler(json)

        val currentUserTurn = AssistantMessage.User(userMessage)
        val systemPrompt = history.filterIsInstance<AssistantMessage.System>().firstOrNull()
            ?: AssistantMessage.System("")
        val rawTranscript = history.filterNot { it is AssistantMessage.System }
        val budgeted = historyBudgeter.build(systemPrompt, rawTranscript, currentUserTurn)

        var modelMessages: List<AssistantMessage> = budgeted.messages
        val newTurnMessages = mutableListOf<AssistantMessage>(currentUserTurn)
        var visibleOutputStarted = false
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
            modelMessages = modelMessages + newAssistantMsg
            newTurnMessages.add(newAssistantMsg)

            if (stream.finishReason == null) {
                emit(LoopEvent.Finished(LoopOutcome.FAILED, history + newTurnMessages, retryAvailable = false))
                return@flow
            }

            val isTerminal = stream.finishReason == FinishReason.STOP ||
                (stream.finishReason != FinishReason.TOOL_CALLS && stream.toolCallDeltas.isEmpty())
            if (isTerminal) {
                emit(LoopEvent.Finished(LoopOutcome.COMPLETED, history + newTurnMessages))
                return@flow
            }

            val toolResults = executeTools(assembledResults, validCalls, toolDescriptors)
            for (result in toolResults) {
                val newToolMsg = AssistantMessage.Tool(
                    toolCallId = result.toolCallId,
                    content = result.toModelContent(),
                )
                modelMessages = modelMessages + newToolMsg
                newTurnMessages.add(newToolMsg)
            }
            iteration++
        }

        emit(LoopEvent.Finished(LoopOutcome.MAX_ITERATIONS, history + newTurnMessages))
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

            when (
                val decision = retryPolicy.decide(
                    LoopFailureContext(failure.kind, visibleOutputStarted, retryCount)
                )
            ) {
                is RetryDecision.RetryNow -> {
                    delay(decision.backoffMs)
                    retryCount++
                }
                is RetryDecision.ShowManualRetry -> {
                    val failedHistory = messagesWithPartialText(fullHistory, assistantText.toString())
                    emit(LoopEvent.Finished(LoopOutcome.FAILED, failedHistory, retryAvailable = true))
                    return null
                }
                is RetryDecision.DoNotRetry -> {
                    val failedHistory = messagesWithPartialText(fullHistory, assistantText.toString())
                    emit(LoopEvent.Finished(LoopOutcome.FAILED, failedHistory, retryAvailable = false))
                    return null
                }
            }
        }
    }

    private suspend fun FlowCollector<LoopEvent>.executeTools(
        assembledResults: List<ToolCallAssembler.AssemblyResult>,
        validCalls: List<ToolCall>,
        toolDescriptors: List<ToolDescriptor>,
    ): List<ToolResult> {
        val toolResults = mutableListOf<ToolResult>()
        for (r in assembledResults) {
            if (r is ToolCallAssembler.AssemblyResult.MalformedArguments) {
                val result = ToolResult.ValidationError(r.callId, "Malformed arguments for ${r.toolName}")
                toolResults += result
                emit(LoopEvent.ToolCallFinished(result))
            }
        }
        for (call in validCalls) {
            emit(LoopEvent.ToolCallStarted(call))
            val descriptor = toolDescriptors.find { it.name == call.name }
            val result = when {
                descriptor == null ->
                    ToolResult.ValidationError(call.id, "Unknown tool: ${call.name}")
                descriptor.safetyLevel == ToolSafetyLevel.UNSAFE -> {
                    emit(LoopEvent.BlockedBySafety(call))
                    ToolResult.RejectedBySafety(call.id)
                }
                else -> toolRegistry.execute(call)
            }
            toolResults += result
            if (result !is ToolResult.RejectedBySafety) {
                emit(LoopEvent.ToolCallFinished(result))
            }
        }
        return toolResults
    }

    private data class StreamResult(
        val toolCallDeltas: List<AssistantEvent.ToolCallDelta>,
        val assistantText: String,
        val finishReason: FinishReason?,
        val visibleOutputStarted: Boolean,
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

    companion object {
        internal const val MAX_ITERATIONS = 5
    }
}
