package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AgenticLoopImplTest {
    private val json = assistantJsonForTests()
    private val context = SessionContext(siteId = 1L, catalogSnapshot = CatalogSnapshot(ToolScope.GLOBAL, emptyList()))
    private val history = listOf<AssistantMessage>(AssistantMessage.System("You are a helpful assistant."))

    private fun passThroughBudgeter(): HistoryBudgeter = HistoryBudgeter { system, transcript, user ->
        BudgetedHistory(messages = listOf(system) + transcript + user)
    }

    private fun loopWith(
        vararg turnResponses: Flow<AssistantEvent>,
        registry: ToolRegistry = NoOpToolRegistry(),
        budgeter: HistoryBudgeter = passThroughBudgeter(),
    ): AgenticLoopImpl {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest) =
                turnResponses[minOf(callCount++, turnResponses.size - 1)]
        }
        return AgenticLoopImpl(service, registry, ConservativeRetryPolicy, budgeter, json)
    }

    private fun stubRegistry(
        result: ToolResult = ToolResult.Success("call_1", buildJsonObject { put("ok", true) }),
    ): ToolRegistry = object : ToolRegistry {
        override fun descriptors() = emptyList<ToolDescriptor>()
        override suspend fun execute(call: ToolCall) = result
    }

    private fun safeEchoDescriptor() = ToolDescriptor(
        name = "echo",
        description = "Echoes the input",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    private fun contextWithTools(vararg tools: ToolDescriptor) =
        SessionContext(siteId = 1L, catalogSnapshot = CatalogSnapshot(ToolScope.GLOBAL, tools.toList()))

    @Test
    fun `given model returns STOP finish reason, when running turn, then Finished with COMPLETED is emitted`() = runTest {
        val loop = loopWith(
            flowOf(AssistantEvent.TextDelta("Hello"), AssistantEvent.Finish(FinishReason.STOP))
        )

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
    }

    @Test
    fun `given model never stops and always requests tool calls, when running turn, then Finished with MAX_ITERATIONS is emitted`() = runTest {
        val singleIteration = flow {
            emit(AssistantEvent.ToolCallDelta(index = 0, id = "call_x", name = "echo", argumentsDelta = "{}"))
            emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
        }
        val registry = stubRegistry(result = ToolResult.Success("call_x", buildJsonObject { }))
        val loop = loopWith(
            singleIteration,
            singleIteration,
            singleIteration,
            singleIteration,
            singleIteration,
            registry = registry
        )

        val events = loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor())).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.MAX_ITERATIONS)
    }

    @Test
    fun `given malformed tool call arguments, when running turn, then ToolCallFinished with ValidationError is emitted`() = runTest {
        val loop = loopWith(
            flow {
                emit(
                    AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "echo", argumentsDelta = "{bad json")
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flowOf(AssistantEvent.Finish(FinishReason.STOP))
        )

        val events = loop.runTurn("conv", "go", history, context).toList()

        val malformedError = events.filterIsInstance<LoopEvent.ToolCallFinished>()
            .firstOrNull { it.result is ToolResult.ValidationError }
        assertThat(malformedError).isNotNull
    }

    @Test
    fun `given malformed tool call arguments, when running another turn, then tool message references prior assistant tool call id`() = runTest {
        val loop = loopWith(
            flow {
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = 0,
                        id = "call_bad",
                        name = "echo",
                        argumentsDelta = """{"msg":"""",
                    )
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flowOf(AssistantEvent.Finish(FinishReason.STOP))
        )

        val finished = loop.runTurn("conv", "go", history, context).toList()
            .filterIsInstance<LoopEvent.Finished>()
            .last()

        finished.updatedHistory.forEachIndexed { index, message ->
            if (message is AssistantMessage.Tool) {
                val priorIds = finished.updatedHistory.subList(0, index)
                    .filterIsInstance<AssistantMessage.Assistant>()
                    .flatMap { assistant -> assistant.toolCalls.map(ToolCall::id) }
                assertThat(priorIds).contains(message.toolCallId)
            }
        }
    }

    @Test
    fun `given tool returns Success with uiStructured, when result is re-submitted, then only structured is in Tool message`() = runTest {
        val structuredPayload = buildJsonObject { put("orders", 3) }
        val uiOnlyPayload = buildJsonObject { put("cards", "rich_card_data") }
        var secondCallMessages: List<AssistantMessage>? = null
        val service = object : ChatService {
            private var count = 0
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                count++
                return if (count == 1) {
                    flow {
                        emit(
                            AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "echo", argumentsDelta = "{}")
                        )
                        emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
                    }
                } else {
                    secondCallMessages = request.messages
                    flowOf(AssistantEvent.Finish(FinishReason.STOP))
                }
            }
        }
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(safeEchoDescriptor())
            override suspend fun execute(call: ToolCall) =
                ToolResult.Success(call.id, structured = structuredPayload, uiStructured = uiOnlyPayload)
        }
        val loop = AgenticLoopImpl(service, registry, ConservativeRetryPolicy, passThroughBudgeter(), json)

        loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor())).toList()

        val toolMsg = secondCallMessages?.filterIsInstance<AssistantMessage.Tool>()?.firstOrNull()
        assertThat(toolMsg).isNotNull
        assertThat(requireNotNull(toolMsg).content).isEqualTo(structuredPayload.toString())
        assertThat(toolMsg.content).doesNotContain("cards")
    }

    @Test
    fun `given unknown tool name from model, when running turn, then ValidationError is produced without registry execution`() = runTest {
        var registryExecuted = false
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(safeEchoDescriptor())
            override suspend fun execute(call: ToolCall): ToolResult {
                registryExecuted = true
                return ToolResult.Success(call.id, buildJsonObject { })
            }
        }
        val loop = loopWith(
            flow {
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = 0,
                        id = "call_1",
                        name = "nonexistent_tool",
                        argumentsDelta = "{}"
                    )
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flowOf(AssistantEvent.Finish(FinishReason.STOP)),
            registry = registry
        )

        val events = loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor())).toList()

        assertThat(registryExecuted).isFalse
        val validationErrors = events.filterIsInstance<LoopEvent.ToolCallFinished>()
            .map { it.result }
            .filterIsInstance<ToolResult.ValidationError>()
        assertThat(validationErrors).isNotEmpty
    }

    @Test
    fun `given validation error reason containing quotes, when result is re-submitted, then tool message content is valid json`() = runTest {
        val quotedToolName = """ev"il"""
        var secondCallMessages: List<AssistantMessage>? = null
        val service = object : ChatService {
            private var count = 0

            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                count++
                return if (count == 1) {
                    flow {
                        emit(
                            AssistantEvent.ToolCallDelta(
                                index = 0,
                                id = "call_1",
                                name = quotedToolName,
                                argumentsDelta = "{}",
                            )
                        )
                        emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
                    }
                } else {
                    secondCallMessages = request.messages
                    flowOf(AssistantEvent.Finish(FinishReason.STOP))
                }
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        loop.runTurn("conv", "go", history, context).toList()

        val toolMsg = secondCallMessages?.filterIsInstance<AssistantMessage.Tool>()?.single()
        val parsed = json.parseToJsonElement(requireNotNull(toolMsg).content).jsonObject
        assertThat(parsed["error"]?.jsonPrimitive?.content).isEqualTo("Unknown tool: $quotedToolName")
    }

    @Test
    fun `given stream fails with network error before visible output, when running turn, then stream is retried and completes`() = runTest {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                callCount++
                return when (callCount) {
                    1 -> flowOf(AssistantEvent.Failed(ChatStreamError.NETWORK))
                    else -> flowOf(AssistantEvent.TextDelta("ok"), AssistantEvent.Finish(FinishReason.STOP))
                }
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
        assertThat(callCount).isGreaterThan(1)
    }

    @Test
    fun `given stream fails with auth error before visible output, when running turn, then FAILED is emitted without retry and retryAvailable is false`() = runTest {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                callCount++
                return flowOf(AssistantEvent.Failed(ChatStreamError.AUTH))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
        assertThat(finished.retryAvailable).isFalse
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `given network error exhausts MAX_AUTO_RETRIES with no visible output, when running turn, then FAILED with retryAvailable true`() = runTest {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                callCount++
                return flowOf(AssistantEvent.Failed(ChatStreamError.NETWORK))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
        assertThat(finished.retryAvailable).isTrue
        assertThat(callCount).isEqualTo(ConservativeRetryPolicy.MAX_AUTO_RETRIES + 1)
    }

    @Test
    fun `given stream emits partial text then fails with network error, when running turn, then retryAvailable is true`() = runTest {
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
                emit(AssistantEvent.TextDelta("partial"))
                emit(AssistantEvent.Failed(ChatStreamError.NETWORK))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
        assertThat(finished.retryAvailable).isTrue
    }

    @Test
    fun `given stream emits partial text then fails, when running turn, then partial text is in updatedHistory`() = runTest {
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
                emit(AssistantEvent.TextDelta("partial"))
                emit(AssistantEvent.Failed(ChatStreamError.NETWORK))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        val partialMsg = finished.updatedHistory.filterIsInstance<AssistantMessage.Assistant>().lastOrNull()
        assertThat(partialMsg).isNotNull
        assertThat(requireNotNull(partialMsg).content).isEqualTo("partial")
    }

    @Test
    fun `given stub SAFE tool, when loop runs one tool call then STOP, then completes with tool result in history`() = runTest {
        val echoResult = buildJsonObject { put("echoed", "hello") }
        val registry = stubRegistry(result = ToolResult.Success("call_1", echoResult))
        val loop = loopWith(
            flow {
                emit(AssistantEvent.TextDelta("I'll echo that."))
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = 0,
                        id = "call_1",
                        name = "echo",
                        argumentsDelta = """{"msg":"hello"}"""
                    )
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flow {
                emit(AssistantEvent.TextDelta("Done."))
                emit(AssistantEvent.Finish(FinishReason.STOP))
            },
            registry = registry
        )

        val events = loop.runTurn("conv", "echo hello", history, contextWithTools(safeEchoDescriptor())).toList()

        assertThat(events.filterIsInstance<LoopEvent.AssistantTextDelta>().map { it.text })
            .containsExactly("I'll echo that.", "Done.")
        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>()).hasSize(1)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>()).hasSize(1)
        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
        val toolMessages = finished.updatedHistory.filterIsInstance<AssistantMessage.Tool>()
        assertThat(toolMessages).hasSize(1)
        assertThat(toolMessages[0].content).contains("echoed")
    }

    @Test
    fun `given UNSAFE tool, when loop runs tool call, then BlockedBySafety is emitted and ToolCallFinished is not`() = runTest {
        val unsafeDescriptor = ToolDescriptor(
            name = "dangerous",
            description = "A dangerous tool",
            inputSchema = buildJsonObject { },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        val registry = stubRegistry()
        val loop = loopWith(
            flow {
                emit(AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "dangerous", argumentsDelta = "{}"))
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flowOf(AssistantEvent.Finish(FinishReason.STOP)),
            registry = registry,
        )
        val context = contextWithTools(unsafeDescriptor)

        val events = loop.runTurn("conv", "go", history, context).toList()

        assertThat(events.filterIsInstance<LoopEvent.BlockedBySafety>()).hasSize(1)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>()).isEmpty()
    }

    @Test
    fun `given budgeter trims history, when running turn, then model receives only budgeted messages`() = runTest {
        val bigHistory = listOf(
            AssistantMessage.System("sys"),
            AssistantMessage.User("turn 1"),
            AssistantMessage.Assistant("response 1"),
            AssistantMessage.User("turn 2"),
            AssistantMessage.Assistant("response 2"),
        )
        val twoMessageBudgeter = HistoryBudgeter { system, _, user ->
            BudgetedHistory(messages = listOf(system, user))
        }
        var capturedMessages: List<AssistantMessage>? = null
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                capturedMessages = request.messages
                return flowOf(AssistantEvent.TextDelta("Hi"), AssistantEvent.Finish(FinishReason.STOP))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, twoMessageBudgeter, json)

        loop.runTurn("conv", "hi", bigHistory, context).toList()

        assertThat(capturedMessages).hasSize(2)
        assertThat(capturedMessages?.first()).isEqualTo(AssistantMessage.System("sys"))
        assertThat(capturedMessages?.last()).isEqualTo(AssistantMessage.User("hi"))
    }

    @Test
    fun `given budgeter trims history, when running turn, then updatedHistory contains full prior history plus new turn messages`() = runTest {
        val bigHistory = listOf(
            AssistantMessage.System("sys"),
            AssistantMessage.User("turn 1"),
            AssistantMessage.Assistant("response 1"),
        )
        val droppingBudgeter = HistoryBudgeter { system, _, user ->
            BudgetedHistory(messages = listOf(system, user))
        }
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> =
                flowOf(AssistantEvent.TextDelta("ok"), AssistantEvent.Finish(FinishReason.STOP))
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, droppingBudgeter, json)

        val events = loop.runTurn("conv", "hi", bigHistory, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.updatedHistory).hasSize(bigHistory.size + 2)
        assertThat(finished.updatedHistory.take(bigHistory.size)).isEqualTo(bigHistory)
        assertThat(finished.updatedHistory[bigHistory.size]).isEqualTo(AssistantMessage.User("hi"))
        assertThat(finished.updatedHistory[bigHistory.size + 1])
            .isEqualTo(AssistantMessage.Assistant(content = "ok", toolCalls = emptyList()))
    }

    @Test
    fun `given history with no system message, when running turn, then loop completes with empty system prompt`() = runTest {
        val historyWithoutSystem = listOf(
            AssistantMessage.User("prior question"),
            AssistantMessage.Assistant("prior answer"),
        )
        val loop = loopWith(
            flowOf(AssistantEvent.TextDelta("hi"), AssistantEvent.Finish(FinishReason.STOP))
        )

        val events = loop.runTurn("conv", "hello", historyWithoutSystem, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
    }

    @Test
    fun `given stream fails with auth error, when running turn, then Finished carries AssistantError Auth`() = runTest {
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> =
                flowOf(AssistantEvent.Failed(ChatStreamError.AUTH))
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
        assertThat(finished.error).isEqualTo(AssistantError.Auth)
    }

    @Test
    fun `given stream fails with INVALID_STREAM after partial text, when running turn, then Finished carries UpstreamFailure`() = runTest {
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
                emit(AssistantEvent.TextDelta("partial"))
                emit(AssistantEvent.Failed(ChatStreamError.INVALID_STREAM))
            }
        }
        val loop = AgenticLoopImpl(service, NoOpToolRegistry(), ConservativeRetryPolicy, passThroughBudgeter(), json)

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
        assertThat(finished.retryAvailable).isTrue
        assertThat(finished.error).isEqualTo(AssistantError.UpstreamFailure)
    }

    @Test
    fun `given stream completes successfully, when running turn, then Finished error is null`() = runTest {
        val loop = loopWith(
            flowOf(AssistantEvent.TextDelta("Hi"), AssistantEvent.Finish(FinishReason.STOP))
        )

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
        assertThat(finished.error).isNull()
    }
}
