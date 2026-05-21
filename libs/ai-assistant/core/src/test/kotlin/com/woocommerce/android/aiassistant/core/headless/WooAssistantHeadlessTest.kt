package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.BudgetedHistory
import com.woocommerce.android.aiassistant.core.loop.CatalogSnapshot
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.time.TimeSource

class WooAssistantHeadlessTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun `given scripted turns and recording registry, when running scenario, then result captures assistant text and tool trace`() =
        runTest {
            val toolDescriptor = toolDescriptor("orders_list", ToolSafetyLevel.SAFE)
            val registry = RecordingHeadlessToolRegistry(
                descriptors = listOf(toolDescriptor),
                results = mapOf(
                    "orders_list" to ToolResult.Success(
                        toolCallId = "call_1",
                        structured = buildJsonObject { put("count", 2) },
                    )
                ),
            )
            val harness = harness(orderListResponses(), registry)

            val result = harness.runScenario(
                scenario = scenario(
                    id = "orders-processing",
                    userMessage = "How many processing orders do I have?",
                    toolDescriptor = toolDescriptor,
                )
            )

            assertThat(result.scenarioId).isEqualTo("orders-processing")
            assertThat(result.turns).hasSize(1)
            assertThat(result.turns.single().assistantText)
                .isEqualTo("Checking orders.There are 2 processing orders.")
            assertThat(result.turns.single().toolCalls).containsExactly(
                HeadlessToolCallTrace(
                    id = "call_1",
                    name = "orders_list",
                    arguments = buildJsonObject { put("status", "processing") },
                    safetyLevel = ToolSafetyLevel.SAFE,
                    resultKind = HeadlessToolResultKind.SUCCESS,
                )
            )
            assertThat(registry.calls.map(ToolCall::name)).containsExactly("orders_list")
        }

    @Test
    fun `given unsafe tool and confirming safety, when running scenario, then result captures confirmation handoff`() =
        runTest {
            val toolDescriptor = toolDescriptor("orders_update", ToolSafetyLevel.UNSAFE)
            val registry = RecordingHeadlessToolRegistry(
                descriptors = listOf(toolDescriptor),
                results = mapOf(
                    "orders_update" to ToolResult.Success(
                        toolCallId = "call_1",
                        structured = buildJsonObject { put("ok", true) },
                    )
                ),
            )
            val harness = harness(
                responses = orderUpdateResponses(includeFinalAnswer = true),
                registry = registry,
                safetyOrchestrator = ScriptedHeadlessSafetyOrchestrator(
                    defaultDecision = ConfirmationDecision.CONFIRMED,
                ),
            )

            val result = harness.runScenario(
                scenario = scenario(
                    id = "complete-order",
                    userMessage = "Complete order 42",
                    toolDescriptor = toolDescriptor,
                )
            )

            val turn = result.turns.single()
            assertThat(turn.outcome).isEqualTo(LoopOutcome.COMPLETED)
            assertThat(turn.confirmationRequests).containsExactly(
                HeadlessConfirmationRequestTrace(
                    id = "call_1-confirmation",
                    toolCallId = "call_1",
                    toolName = "orders_update",
                    arguments = buildJsonObject {
                        put("id", 42)
                        put("status", "completed")
                    },
                    safetyLevel = ToolSafetyLevel.UNSAFE,
                )
            )
            assertThat(turn.confirmationResults).containsExactly(
                HeadlessConfirmationResultTrace(
                    requestId = "call_1-confirmation",
                    decision = "CONFIRMED",
                )
            )
            assertThat(turn.toolCalls.map { it.name }).containsExactly("orders_update")
            assertThat(registry.calls.map(ToolCall::name)).containsExactly("orders_update")
        }

    @Test
    fun `given unsafe tool and default headless safety, when running scenario, then tool is cancelled`() =
        runTest {
            val toolDescriptor = toolDescriptor("orders_update", ToolSafetyLevel.UNSAFE)
            val registry = RecordingHeadlessToolRegistry(
                descriptors = listOf(toolDescriptor),
                results = mapOf(
                    "orders_update" to ToolResult.Success(
                        toolCallId = "call_1",
                        structured = buildJsonObject { put("ok", true) },
                    )
                ),
            )
            val harness = harness(orderUpdateResponses(includeFinalAnswer = false), registry)

            val result = harness.runScenario(
                scenario = scenario(
                    id = "cancel-order-update",
                    userMessage = "Complete order 42",
                    toolDescriptor = toolDescriptor,
                )
            )

            val turn = result.turns.single()
            assertThat(turn.outcome).isEqualTo(LoopOutcome.STOPPED)
            assertThat(turn.confirmationResults.single().decision).isEqualTo("CANCELLED")
            assertThat(turn.errors).isEmpty()
            assertThat(turn.toolCalls).containsExactly(
                HeadlessToolCallTrace(
                    id = "call_1",
                    name = "orders_update",
                    arguments = buildJsonObject {
                        put("id", 42)
                        put("status", "completed")
                    },
                    safetyLevel = ToolSafetyLevel.UNSAFE,
                    resultKind = HeadlessToolResultKind.REJECTED_BY_SAFETY,
                )
            )
            assertThat(registry.calls).isEmpty()
        }

    private fun toolDescriptor(
        name: String,
        safetyLevel: ToolSafetyLevel,
    ) = ToolDescriptor(
        name = name,
        description = "Test tool",
        inputSchema = buildJsonObject { },
        safetyLevel = safetyLevel,
    )

    private fun orderListResponses() = listOf(
        listOf(
            AssistantEvent.TextDelta("Checking orders."),
            AssistantEvent.ToolCallDelta(
                index = 0,
                id = "call_1",
                name = "orders_list",
                argumentsDelta = """{"status":"processing"}""",
            ),
            AssistantEvent.Finish(FinishReason.TOOL_CALLS),
        ),
        listOf(
            AssistantEvent.TextDelta("There are 2 processing orders."),
            AssistantEvent.Finish(FinishReason.STOP),
        ),
    )

    private fun orderUpdateResponses(includeFinalAnswer: Boolean): List<List<AssistantEvent>> {
        val toolCallResponse = listOf(
            AssistantEvent.ToolCallDelta(
                index = 0,
                id = "call_1",
                name = "orders_update",
                argumentsDelta = """{"id":42,"status":"completed"}""",
            ),
            AssistantEvent.Finish(FinishReason.TOOL_CALLS),
        )
        val finalAnswer = listOf(
            AssistantEvent.TextDelta("Order updated."),
            AssistantEvent.Finish(FinishReason.STOP),
        )
        return if (includeFinalAnswer) {
            listOf(toolCallResponse, finalAnswer)
        } else {
            listOf(toolCallResponse)
        }
    }

    private fun harness(
        responses: List<List<AssistantEvent>>,
        registry: RecordingHeadlessToolRegistry,
        safetyOrchestrator: ScriptedHeadlessSafetyOrchestrator = ScriptedHeadlessSafetyOrchestrator(),
    ) = WooAssistantHeadless(
        chatService = ScriptedHeadlessChatService(responses),
        toolRegistry = registry,
        retryPolicy = ConservativeRetryPolicy,
        historyBudgeter = passThroughBudgeter(),
        json = json,
        timeSource = TimeSource.Monotonic,
        safetyOrchestrator = safetyOrchestrator,
    )

    private fun scenario(
        id: String,
        userMessage: String,
        toolDescriptor: ToolDescriptor,
    ) = HeadlessScenario(
        id = id,
        turns = listOf(
            HeadlessTurnSpec(
                userMessage = userMessage,
                hardChecks = emptyList(),
            )
        ),
        initialHistory = listOf(AssistantMessage.System("You are a helpful commerce assistant.")),
        context = SessionContext(
            siteId = 1L,
            catalogSnapshot = CatalogSnapshot(ToolScope.GLOBAL, listOf(toolDescriptor)),
        ),
    )

    private fun passThroughBudgeter() = HistoryBudgeter { system, transcript, user ->
        BudgetedHistory(listOf(system) + transcript + user)
    }
}
