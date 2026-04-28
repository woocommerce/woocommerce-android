package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class AgenticLoopImpl(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val retryPolicy: RetryPolicy,
    private val json: Json,
) : AgenticLoop {

    override fun runTurn(
        conversationId: String,
        userMessage: String,
        history: List<AssistantMessage>,
        context: SessionContext,
    ): Flow<LoopEvent> = flow {
        val toolDescriptors = toolRegistry.descriptors()
        val toolDefs = toolDescriptors.map { it.toToolDefinition() }
        val assembler = ToolCallAssembler(json)
        val seenSignatures = mutableSetOf<Pair<String, String>>()

        var messages: List<AssistantMessage> = history + AssistantMessage.User(userMessage)
        var visibleOutputStarted = false
        var iteration = 0

        while (iteration < MAX_ITERATIONS) {
            val request = ChatRequest(messages = messages, tools = toolDefs)
            var retryCount = 0

            val toolCallDeltas = mutableListOf<AssistantEvent.ToolCallDelta>()
            val assistantText = StringBuilder()
            var finishReason: FinishReason? = null
            var streamFailed: AssistantEvent.Failed? = null

            while (true) {
                toolCallDeltas.clear()
                assistantText.clear()
                finishReason = null
                streamFailed = null

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

                val failure = streamFailed ?: break

                when (val decision = retryPolicy.decide(
                    LoopFailureContext(failure.kind, visibleOutputStarted, retryCount)
                )) {
                    is RetryDecision.RetryNow -> {
                        delay(decision.backoffMs)
                        retryCount++
                    }
                    is RetryDecision.ShowManualRetry -> {
                        val failedHistory = messagesWithPartialText(messages, assistantText)
                        emit(LoopEvent.Finished(LoopOutcome.FAILED, failedHistory, retryAvailable = true))
                        return@flow
                    }
                    is RetryDecision.DoNotRetry -> {
                        val failedHistory = messagesWithPartialText(messages, assistantText)
                        emit(LoopEvent.Finished(LoopOutcome.FAILED, failedHistory, retryAvailable = false))
                        return@flow
                    }
                }
            }

            val assembledResults = assembler.assemble(toolCallDeltas)
            val validCalls = assembledResults
                .filterIsInstance<ToolCallAssembler.AssemblyResult.Success>()
                .map { it.call }

            messages = messages + AssistantMessage.Assistant(
                content = assistantText.toString().takeIf { it.isNotEmpty() },
                toolCalls = validCalls,
            )

            if (finishReason == null) {
                emit(LoopEvent.Finished(LoopOutcome.FAILED, messages, retryAvailable = false))
                return@flow
            }

            if (finishReason == FinishReason.STOP || (finishReason != FinishReason.TOOL_CALLS && toolCallDeltas.isEmpty())) {
                emit(LoopEvent.Finished(LoopOutcome.COMPLETED, messages))
                return@flow
            }

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

                val signature = call.name to call.arguments.toString()
                if (signature in seenSignatures) {
                    val result = ToolResult.ValidationError(call.id, "Duplicate call: ${call.name}")
                    toolResults += result
                    emit(LoopEvent.ToolCallFinished(result))
                    continue
                }
                seenSignatures += signature

                val descriptor = toolDescriptors.find { it.name == call.name }
                if (descriptor == null) {
                    val result = ToolResult.ValidationError(call.id, "Unknown tool: ${call.name}")
                    toolResults += result
                    emit(LoopEvent.ToolCallFinished(result))
                    continue
                }

                if (descriptor.safetyLevel == ToolSafetyLevel.UNSAFE) {
                    emit(LoopEvent.AwaitingConfirmation(call))
                    val result = ToolResult.RejectedBySafety(call.id)
                    toolResults += result
                    emit(LoopEvent.ToolCallFinished(result))
                    continue
                }

                val result = toolRegistry.execute(call)
                toolResults += result
                emit(LoopEvent.ToolCallFinished(result))
            }

            for (result in toolResults) {
                messages = messages + AssistantMessage.Tool(
                    toolCallId = result.toolCallId,
                    content = result.toModelContent(),
                )
            }

            iteration++
        }

        emit(LoopEvent.Finished(LoopOutcome.MAX_ITERATIONS, messages))
    }

    private fun messagesWithPartialText(
        messages: List<AssistantMessage>,
        partial: StringBuilder,
    ): List<AssistantMessage> = if (partial.isNotEmpty()) {
        messages + AssistantMessage.Assistant(content = partial.toString(), toolCalls = emptyList())
    } else {
        messages
    }

    private fun ToolResult.toModelContent(): String = when (this) {
        is ToolResult.Success -> structured.toString()
        is ToolResult.ValidationError -> """{"error":"$reason"}"""
        is ToolResult.RejectedBySafety -> """{"error":"Action was not approved"}"""
        is ToolResult.TransportError -> """{"error":"Tool execution failed"}"""
    }

    private fun ToolDescriptor.toToolDefinition() = ToolDefinition(
        name = name,
        description = description,
        parameters = inputSchema,
    )

    companion object {
        internal const val MAX_ITERATIONS = 5
    }
}
