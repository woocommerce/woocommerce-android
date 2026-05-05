package com.woocommerce.android.aiassistant.core.loop

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

class ToolReplayTrackerTest {
    private val json = assistantJsonForTests()
    private val history = listOf<AssistantMessage>(AssistantMessage.System("You are a helpful assistant."))

    @Test
    fun `given ToolReplayTracker sees fifth identical call, when running turn, then cap wins over replay`() = runTest {
        val registry = RecordingToolRegistry(
            descriptor = safeEchoDescriptor(),
            resultBuilder = { call ->
                ToolResult.Success(call.id, buildJsonObject { put("from_registry", call.id) })
            }
        )
        val loop = loopWith(
            multiToolCallTurn(
                "call_1" to """{"value":1}""",
                "call_2" to """{"value":1}""",
                "call_3" to """{"value":1}""",
                "call_4" to """{"value":1}""",
                "call_5" to """{"value":1}""",
            ),
            stopTurn(),
            registry = registry,
        )

        val events = loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor())).toList()

        assertThat(registry.executedCalls.map { it.id }).containsExactly("call_1")
        assertThat(events.filterIsInstance<LoopEvent.ToolCallStarted>().map { it.call.id })
            .containsExactly("call_1")

        val results = events.filterIsInstance<LoopEvent.ToolCallFinished>().map { it.result }
        assertThat(results).hasSize(5)
        val capResult = results[4] as ToolResult.ValidationError
        assertThat(capResult.toolCallId).isEqualTo("call_5")
        assertThat(capResult.reason).contains("Tool call limit exceeded for echo")
        assertThat(capResult.reason).contains("already called 4 times this turn")
    }

    @Test
    fun `given ToolReplayTracker replays UI payload, when running turn, then UI payload is preserved`() = runTest {
        val uiPayload = buildJsonObject { put("ui_only", "rich_card_data") }
        val registry = RecordingToolRegistry(
            descriptor = safeEchoDescriptor(),
            resultBuilder = { call ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = buildJsonObject { put("from_registry", call.id) },
                    uiStructured = uiPayload,
                )
            }
        )
        val loop = loopWith(
            multiToolCallTurn(
                "call_1" to """{"b":2,"a":1}""",
                "call_2" to """{"a":1,"b":2}""",
            ),
            stopTurn(),
            registry = registry,
        )

        val events = loop.runTurn("conv", "go", history, contextWithTools(safeEchoDescriptor())).toList()

        val results = events.filterIsInstance<LoopEvent.ToolCallFinished>().map { it.result }
        val original = results[0] as ToolResult.Success
        val replay = results[1] as ToolResult.Success
        assertThat(replay.toolCallId).isEqualTo("call_2")
        assertThat(replay.structured.jsonObject["from_registry"]?.jsonPrimitive?.content).isEqualTo("call_1")
        assertThat(replay.structured.jsonObject["_assistant_runtime_hint"]?.jsonPrimitive?.content)
            .isEqualTo("You already fetched this - use the result above.")
        assertThat(replay.uiStructured).isEqualTo(original.uiStructured)
    }

    private fun loopWith(
        vararg turnResponses: Flow<AssistantEvent>,
        registry: ToolRegistry,
    ): AgenticLoopImpl {
        var callCount = 0
        val service = object : ChatService {
            override fun streamTurn(request: ChatRequest) =
                turnResponses[minOf(callCount++, turnResponses.size - 1)]
        }
        return AgenticLoopImpl(
            chatService = service,
            toolRegistry = registry,
            retryPolicy = ConservativeRetryPolicy,
            historyBudgeter = passThroughBudgeter(),
            json = json,
        )
    }

    private fun passThroughBudgeter(): HistoryBudgeter = HistoryBudgeter { system, transcript, user ->
        BudgetedHistory(messages = listOf(system) + transcript + user)
    }

    private fun safeEchoDescriptor() = ToolDescriptor(
        name = "echo",
        description = "Echoes the input",
        inputSchema = buildJsonObject { },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    private fun contextWithTools(vararg tools: ToolDescriptor) =
        SessionContext(siteId = 1L, catalogSnapshot = CatalogSnapshot(ToolScope.GLOBAL, tools.toList()))

    private fun multiToolCallTurn(vararg calls: Pair<String, String>): Flow<AssistantEvent> = flow {
        calls.forEachIndexed { index, (callId, arguments) ->
            emit(AssistantEvent.ToolCallDelta(index = index, id = callId, name = "echo", argumentsDelta = arguments))
        }
        emit(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
    }

    private fun stopTurn(): Flow<AssistantEvent> = flowOf(AssistantEvent.Finish(FinishReason.STOP))

    private class RecordingToolRegistry(
        private val descriptor: ToolDescriptor,
        private val resultBuilder: (ToolCall) -> ToolResult,
    ) : ToolRegistry {
        val executedCalls = mutableListOf<ToolCall>()

        override fun descriptors() = listOf(descriptor)

        override suspend fun execute(call: ToolCall): ToolResult {
            executedCalls += call
            return resultBuilder(call)
        }
    }
}
