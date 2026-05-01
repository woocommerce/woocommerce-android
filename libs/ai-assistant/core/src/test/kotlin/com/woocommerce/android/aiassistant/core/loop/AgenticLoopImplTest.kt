package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestratorImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        safetyOrchestrator: SafetyOrchestrator = SafetyOrchestratorImpl(),
    ): AgenticLoopImpl {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest) =
                turnResponses[minOf(callCount++, turnResponses.size - 1)]
        }
        return AgenticLoopImpl(service, registry, ConservativeRetryPolicy, budgeter, safetyOrchestrator, json)
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
        val loop = AgenticLoopImpl(
            service,
            registry,
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

        val events = loop.runTurn("conv", "hi", history, context).toList()

        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        val partialMsg = finished.updatedHistory.filterIsInstance<AssistantMessage.Assistant>().lastOrNull()
        assertThat(partialMsg).isNotNull
        assertThat(requireNotNull(partialMsg).content).isEqualTo("partial")
    }

    @Test
    fun `given stream cancellation before visible output, when running turn, then stopped cancelled finish is emitted without retry`() =
        runTest {
            val loop = loopWith(
                flowOf(AssistantEvent.Failed(ChatStreamError.CANCELLED))
            )

            val events = loop.runTurn("conv", "hi", history, context).toList()

            assertThat(events.filterIsInstance<LoopEvent.Failed>().single().error)
                .isEqualTo(AssistantError.Cancelled)
            val finished = events.filterIsInstance<LoopEvent.Finished>().last()
            assertThat(finished.outcome).isEqualTo(LoopOutcome.STOPPED)
            assertThat(finished.retryAvailable).isFalse()
            assertThat(finished.error).isEqualTo(AssistantError.Cancelled)
            assertThat(finished.updatedHistory).containsExactly(
                AssistantMessage.System("You are a helpful assistant."),
                AssistantMessage.User("hi"),
            )
        }

    @Test
    fun `given stream emits partial text then cancellation, when running turn, then partial text is preserved and finish is stopped without retry`() =
        runTest {
            val loop = loopWith(
                flow {
                    emit(AssistantEvent.TextDelta("partial"))
                    emit(AssistantEvent.Failed(ChatStreamError.CANCELLED))
                }
            )

            val events = loop.runTurn("conv", "hi", history, context).toList()

            assertThat(events.filterIsInstance<LoopEvent.AssistantTextDelta>().single().text)
                .isEqualTo("partial")
            assertThat(events.filterIsInstance<LoopEvent.Failed>().single().error)
                .isEqualTo(AssistantError.Cancelled)
            val finished = events.filterIsInstance<LoopEvent.Finished>().last()
            assertThat(finished.outcome).isEqualTo(LoopOutcome.STOPPED)
            assertThat(finished.retryAvailable).isFalse()
            assertThat(finished.error).isEqualTo(AssistantError.Cancelled)
            assertThat(finished.updatedHistory).containsExactly(
                AssistantMessage.System("You are a helpful assistant."),
                AssistantMessage.User("hi"),
                AssistantMessage.Assistant(content = "partial", toolCalls = emptyList()),
            )
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
        assertThat(events.filterIsInstance<LoopEvent.ConfirmationRequested>()).isEmpty()
        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
        val toolMessages = finished.updatedHistory.filterIsInstance<AssistantMessage.Tool>()
        assertThat(toolMessages).hasSize(1)
        assertThat(toolMessages[0].content).contains("echoed")
    }

    @Test
    fun `given UNSAFE tool, when loop runs tool call, then confirmation request is emitted before execution`() = runTest {
        val unsafeDescriptor = ToolDescriptor(
            name = "orders_update",
            description = "Updates an order",
            inputSchema = buildJsonObject { },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        var registryExecuted = false
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(unsafeDescriptor)
            override suspend fun execute(call: ToolCall): ToolResult {
                registryExecuted = true
                return ToolResult.Success(call.id, buildJsonObject { put("ok", true) })
            }
        }
        val safetyOrchestrator = SafetyOrchestratorImpl()
        val loop = loopWith(
            flow {
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = 0,
                        id = "call_1",
                        name = "orders_update",
                        argumentsDelta = """{"id":42,"status":"processing"}""",
                    )
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            flowOf(AssistantEvent.Finish(FinishReason.STOP)),
            registry = registry,
            safetyOrchestrator = safetyOrchestrator,
        )
        val context = contextWithTools(unsafeDescriptor)
        val events = mutableListOf<LoopEvent>()

        val job = launch {
            loop.runTurn("conv", "go", history, context).toList(events)
        }

        runCurrent()
        val request = events.filterIsInstance<LoopEvent.ConfirmationRequested>().single().request
        assertThat(request.id).isNotBlank
        assertThat(request.toolCallId).isEqualTo("call_1")
        assertThat(request.toolName).isEqualTo("orders_update")
        assertThat(request.arguments["status"]?.jsonPrimitive?.content).isEqualTo("processing")
        assertThat(request.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>()).isEmpty()
        assertThat(registryExecuted).isFalse

        safetyOrchestrator.confirm(request.id)
        advanceUntilIdle()

        assertThat(events.filterIsInstance<LoopEvent.ConfirmationResolved>().single().result.decision)
            .isEqualTo(ConfirmationDecision.CONFIRMED)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>()).hasSize(1)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>()).hasSize(1)
        assertThat(registryExecuted).isTrue
        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.COMPLETED)
        job.cancel()
    }

    @Test
    fun `given flow is cancelled after confirmation request, when await is skipped, then pending request is removed`() = runTest {
        val unsafeDescriptor = ToolDescriptor(
            name = "orders_update",
            description = "Updates an order",
            inputSchema = buildJsonObject { },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        var registryExecuted = false
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(unsafeDescriptor)
            override suspend fun execute(call: ToolCall): ToolResult {
                registryExecuted = true
                return ToolResult.Success(call.id, buildJsonObject { put("ok", true) })
            }
        }
        val safetyOrchestrator = SafetyOrchestratorImpl()
        val loop = loopWith(
            flow {
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = 0,
                        id = "call_1",
                        name = "orders_update",
                        argumentsDelta = """{"id":42,"status":"processing"}""",
                    )
                )
                emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
            },
            registry = registry,
            safetyOrchestrator = safetyOrchestrator,
        )
        val events = mutableListOf<LoopEvent>()

        val job = launch {
            loop.runTurn("conv", "go", history, contextWithTools(unsafeDescriptor)).collect { event ->
                events += event
                if (event is LoopEvent.ConfirmationRequested) {
                    cancel()
                }
            }
        }

        advanceUntilIdle()

        val request = events.filterIsInstance<LoopEvent.ConfirmationRequested>().single().request
        assertThat(job.isCancelled).isTrue
        assertThat(registryExecuted).isFalse
        assertThat(safetyOrchestrator.confirm(request.id)).isFalse
    }

    @Test
    fun `given confirmed unsafe tool returns transport error, when running turn, then outcome unknown fails without retry`() =
        runTest {
            val unsafeDescriptor = ToolDescriptor(
                name = "orders_update",
                description = "Updates an order",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )
            val registry = object : ToolRegistry {
                override fun descriptors() = listOf(unsafeDescriptor)

                override suspend fun execute(call: ToolCall): ToolResult =
                    ToolResult.TransportError(toolCallId = call.id, retryable = true)
            }
            val safetyOrchestrator = SafetyOrchestratorImpl()
            val loop = loopWith(
                flow {
                    emit(
                        AssistantEvent.ToolCallDelta(
                            index = 0,
                            id = "call_1",
                            name = "orders_update",
                            argumentsDelta = """{"id":42,"status":"processing"}""",
                        )
                    )
                    emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
                },
                registry = registry,
                safetyOrchestrator = safetyOrchestrator,
            )
            val events = mutableListOf<LoopEvent>()

            val job = launch {
                loop.runTurn("conv", "update order", history, contextWithTools(unsafeDescriptor)).toList(events)
            }

            runCurrent()
            val request = events.filterIsInstance<LoopEvent.ConfirmationRequested>().single().request
            safetyOrchestrator.confirm(request.id)
            advanceUntilIdle()

            val finished = events.filterIsInstance<LoopEvent.Finished>().last()
            assertThat(finished.outcome).isEqualTo(LoopOutcome.FAILED)
            assertThat(finished.retryAvailable).isFalse()
            assertThat(finished.error).isEqualTo(AssistantError.OutcomeUnknown(toolName = "orders_update"))
            assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>().single().result)
                .isEqualTo(ToolResult.TransportError(toolCallId = "call_1", retryable = true))
            assertThat(finished.updatedHistory.filterIsInstance<AssistantMessage.Tool>().single().toolCallId)
                .isEqualTo("call_1")
            job.cancel()
        }

    @Test
    fun `given UNSAFE tool is cancelled, when loop awaits confirmation, then cancelled error is emitted and history is clean`() = runTest {
        val unsafeDescriptor = ToolDescriptor(
            name = "orders_update",
            description = "Updates an order",
            inputSchema = buildJsonObject { },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        var registryExecuted = false
        var secondModelTurnRequested = false
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
                                name = "orders_update",
                                argumentsDelta = """{"id":42,"status":"processing"}""",
                            )
                        )
                        emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
                    }
                } else {
                    secondModelTurnRequested = true
                    flowOf(AssistantEvent.Finish(FinishReason.STOP))
                }
            }
        }
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(unsafeDescriptor)
            override suspend fun execute(call: ToolCall): ToolResult {
                registryExecuted = true
                return ToolResult.Success(call.id, buildJsonObject { put("ok", true) })
            }
        }
        val safetyOrchestrator = SafetyOrchestratorImpl()
        val loop = AgenticLoopImpl(
            service,
            registry,
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            safetyOrchestrator,
            json,
        )
        val events = mutableListOf<LoopEvent>()

        val job = launch {
            loop.runTurn("conv", "go", history, contextWithTools(unsafeDescriptor)).toList(events)
        }

        runCurrent()
        val request = events.filterIsInstance<LoopEvent.ConfirmationRequested>().single().request
        safetyOrchestrator.cancel(request.id)
        advanceUntilIdle()

        assertThat(events.filterIsInstance<LoopEvent.ConfirmationResolved>().single().result.decision)
            .isEqualTo(ConfirmationDecision.CANCELLED)
        assertThat(events.filterIsInstance<LoopEvent.Failed>().single().error)
            .isEqualTo(AssistantError.Cancelled)
        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>()).isEmpty()
        assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>()).isEmpty()
        assertThat(registryExecuted).isFalse
        assertThat(secondModelTurnRequested).isFalse
        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.STOPPED)
        assertThat(finished.retryAvailable).isFalse
        assertThat(finished.updatedHistory.filterIsInstance<AssistantMessage.Tool>()).isEmpty()
        val assistantMessages = finished.updatedHistory.filterIsInstance<AssistantMessage.Assistant>()
        assertThat(assistantMessages.flatMap { it.toolCalls }).isEmpty()
        job.cancel()
    }

    @Test
    fun `given safe tool succeeds before unsafe tool, when unsafe is cancelled, then history is preserved`() = runTest {
        val unsafeDescriptor = ToolDescriptor(
            name = "orders_update",
            description = "Updates an order",
            inputSchema = buildJsonObject { },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        var secondModelTurnRequested = false
        val service = object : ChatService {
            private var count = 0

            override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> {
                count++
                return if (count == 1) {
                    flow {
                        emit(AssistantEvent.TextDelta("I'll do both."))
                        emit(
                            AssistantEvent.ToolCallDelta(
                                index = 0,
                                id = "safe_1",
                                name = "echo",
                                argumentsDelta = """{"msg":"hello"}""",
                            )
                        )
                        emit(
                            AssistantEvent.ToolCallDelta(
                                index = 1,
                                id = "unsafe_1",
                                name = "orders_update",
                                argumentsDelta = """{"id":42,"status":"processing"}""",
                            )
                        )
                        emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
                    }
                } else {
                    secondModelTurnRequested = true
                    flowOf(AssistantEvent.Finish(FinishReason.STOP))
                }
            }
        }
        val registry = object : ToolRegistry {
            override fun descriptors() = listOf(safeEchoDescriptor(), unsafeDescriptor)
            override suspend fun execute(call: ToolCall): ToolResult =
                ToolResult.Success(call.id, buildJsonObject { put("echoed", "hello") })
        }
        val safetyOrchestrator = SafetyOrchestratorImpl()
        val loop = AgenticLoopImpl(
            service,
            registry,
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            safetyOrchestrator,
            json,
        )
        val events = mutableListOf<LoopEvent>()

        val job = launch {
            loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor(), unsafeDescriptor)).toList(events)
        }

        runCurrent()
        val request = events.filterIsInstance<LoopEvent.ConfirmationRequested>().single().request
        safetyOrchestrator.cancel(request.id)
        advanceUntilIdle()

        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>().map { it.call.id })
            .containsExactly("safe_1")
        assertThat(events.filterIsInstance<LoopEvent.ToolCallFinished>().map { it.result.toolCallId })
            .containsExactly("safe_1")
        assertThat(events.filterIsInstance<LoopEvent.Failed>().single().error)
            .isEqualTo(AssistantError.Cancelled)
        val finished = events.filterIsInstance<LoopEvent.Finished>().last()
        assertThat(finished.outcome).isEqualTo(LoopOutcome.STOPPED)
        assertThat(finished.retryAvailable).isFalse
        assertThat(secondModelTurnRequested).isFalse

        val assistantToolCalls = finished.updatedHistory.filterIsInstance<AssistantMessage.Assistant>()
            .flatMap { it.toolCalls }
        assertThat(assistantToolCalls.map { it.id }).containsExactly("safe_1")
        assertThat(assistantToolCalls.map { it.id }).doesNotContain("unsafe_1")

        val toolMessages = finished.updatedHistory.filterIsInstance<AssistantMessage.Tool>()
        assertThat(toolMessages.map { it.toolCallId }).containsExactly("safe_1")
        assertThat(toolMessages.map { it.toolCallId }).doesNotContain("unsafe_1")
        assertThat(toolMessages.single().content).contains("echoed")
        job.cancel()
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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            twoMessageBudgeter,
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            droppingBudgeter,
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
        val loop = AgenticLoopImpl(
            service,
            NoOpToolRegistry(),
            ConservativeRetryPolicy,
            passThroughBudgeter(),
            SafetyOrchestratorImpl(),
            json,
        )

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
