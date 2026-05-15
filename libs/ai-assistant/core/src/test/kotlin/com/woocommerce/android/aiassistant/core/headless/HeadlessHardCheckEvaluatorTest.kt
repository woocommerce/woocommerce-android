package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HeadlessHardCheckEvaluatorTest {
    @Test
    fun `given run result, when checks match trace, then evaluator returns passed checks`() {
        val result = HeadlessRunResult(
            scenarioId = "orders-processing",
            turns = listOf(
                HeadlessTurnResult(
                    turnIndex = 0,
                    userMessage = "How many processing orders do I have?",
                    assistantText = "There are 2 processing orders.",
                    outcome = LoopOutcome.COMPLETED,
                    toolCalls = listOf(
                        HeadlessToolCallTrace(
                            id = "call_1",
                            name = "orders_list",
                            arguments = buildJsonObject { },
                            safetyLevel = ToolSafetyLevel.SAFE,
                            resultKind = HeadlessToolResultKind.SUCCESS,
                        )
                    ),
                )
            ),
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_TEXT_CONTAINS, "processing orders"),
                HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALLED, "orders_list"),
                HeadlessHardCheck(HeadlessHardCheckType.TOOL_NOT_CALLED, "orders_update"),
            )
        )

        assertThat(checks).allMatch { it.passed }
    }

    @Test
    fun `given run result, when check does not match trace, then evaluator returns failed check`() {
        val result = HeadlessRunResult(
            scenarioId = "orders-processing",
            turns = listOf(
                HeadlessTurnResult(
                    turnIndex = 0,
                    userMessage = "How many processing orders do I have?",
                    assistantText = "I cannot answer that.",
                    outcome = LoopOutcome.COMPLETED,
                    toolCalls = emptyList(),
                )
            ),
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALLED, "orders_list"))
        )

        assertThat(checks.single().passed).isFalse
        assertThat(checks.single().message).contains("orders_list")
    }

    @Test
    fun `when assistant text not contains is evaluated, then it is case insensitive`() {
        val result = runResult(assistantText = "Here are your recent orders.")

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_TEXT_NOT_CONTAINS, "medieval castles"),
                HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_TEXT_NOT_CONTAINS, "RECENT ORDERS"),
            )
        )

        assertThat(checks.map { it.passed }).containsExactly(true, false)
    }

    @Test
    fun `when tool result kind equals is evaluated, then it matches the named tool result kind`() {
        val result = runResult(
            toolCalls = listOf(
                toolCall("orders_update", HeadlessToolResultKind.REJECTED_BY_SAFETY),
                toolCall("orders_list", HeadlessToolResultKind.SUCCESS),
            )
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(
                    HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                    "orders_update:REJECTED_BY_SAFETY",
                ),
                HeadlessHardCheck(
                    HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                    "orders_update:SUCCESS",
                ),
            )
        )

        assertThat(checks.map { it.passed }).containsExactly(true, false)
    }

    @Test
    fun `when confirmation decision equals is evaluated, then it matches a recorded cancellation`() {
        val result = runResult(
            confirmationResults = listOf(
                HeadlessConfirmationResultTrace(
                    requestId = "call_1-confirmation",
                    decision = "CANCELLED",
                )
            )
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS, "CANCELLED"),
                HeadlessHardCheck(HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS, "CONFIRMED"),
            )
        )

        assertThat(checks.map { it.passed }).containsExactly(true, false)
    }

    @Test
    fun `when tool call count at most is evaluated, then it passes only under the configured maximum`() {
        val result = runResult(
            toolCalls = listOf(
                toolCall("orders_list"),
                toolCall("orders_list"),
                toolCall("show_cards"),
            )
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALL_COUNT_AT_MOST, "orders_list:2"),
                HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALL_COUNT_AT_MOST, "orders_list:1"),
            )
        )

        assertThat(checks.map { it.passed }).containsExactly(true, false)
    }

    @Test
    fun `when tool argument json contains is evaluated, then it matches a recursive json subset`() {
        val result = runResult(
            toolCalls = listOf(
                toolCall(
                    name = "products_list",
                    arguments = json.parseToJsonElement(
                        """
                        {
                          "search": "Cappuccino",
                          "filters": {
                            "status": "publish",
                            "stock": {
                              "state": "instock"
                            }
                          }
                        }
                        """.trimIndent()
                    ).jsonObject,
                )
            )
        )

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(
                HeadlessHardCheck(
                    HeadlessHardCheckType.TOOL_ARGUMENT_JSON_CONTAINS,
                    """products_list:{"filters":{"stock":{"state":"instock"}}}""",
                ),
                HeadlessHardCheck(
                    HeadlessHardCheckType.TOOL_ARGUMENT_JSON_CONTAINS,
                    """products_list:{"filters":{"stock":{"state":"outofstock"}}}""",
                ),
            )
        )

        assertThat(checks.map { it.passed }).containsExactly(true, false)
    }

    @Test
    fun `when assistant refusal is evaluated, then it requires refusal and woocommerce scope language`() {
        val result = runResult(
            assistantText = "I can only help with your WooCommerce store, so I can't write that poem."
        )
        val scopedRedirection = runResult(
            assistantText = "I'm here to help with your WooCommerce store-related questions."
        )
        val scopedAssistance = runResult(
            assistantText = "I'm here to assist with your WooCommerce store-related inquiries."
        )
        val scopedCanHelp = runResult(
            assistantText = "I can help you with questions about your WooCommerce store."
        )
        val missingScope = runResult(assistantText = "I can't write that poem.")

        val checks = HeadlessHardCheckEvaluator.evaluate(
            result,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_REFUSAL, "woocommerce_scope"))
        ) + HeadlessHardCheckEvaluator.evaluate(
            scopedRedirection,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_REFUSAL, "woocommerce_scope"))
        ) + HeadlessHardCheckEvaluator.evaluate(
            scopedAssistance,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_REFUSAL, "woocommerce_scope"))
        ) + HeadlessHardCheckEvaluator.evaluate(
            scopedCanHelp,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_REFUSAL, "woocommerce_scope"))
        ) + HeadlessHardCheckEvaluator.evaluate(
            missingScope,
            listOf(HeadlessHardCheck(HeadlessHardCheckType.ASSISTANT_REFUSAL, "woocommerce_scope"))
        )

        assertThat(checks.map { it.passed }).containsExactly(true, true, true, true, false)
    }

    @Test
    fun `when declined write checks are evaluated, then stopped cancelled and rejected safety pass together`() {
        val declinedWrite = runResult(
            outcome = LoopOutcome.STOPPED,
            toolCalls = listOf(toolCall("orders_update", HeadlessToolResultKind.REJECTED_BY_SAFETY)),
            confirmationResults = listOf(
                HeadlessConfirmationResultTrace(
                    requestId = "call_1-confirmation",
                    decision = "CANCELLED",
                )
            )
        )
        val completedWrite = runResult(
            outcome = LoopOutcome.COMPLETED,
            toolCalls = listOf(toolCall("orders_update", HeadlessToolResultKind.SUCCESS)),
            confirmationResults = listOf(
                HeadlessConfirmationResultTrace(
                    requestId = "call_1-confirmation",
                    decision = "CONFIRMED",
                )
            )
        )
        val checks = listOf(
            HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "STOPPED"),
            HeadlessHardCheck(HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS, "CANCELLED"),
            HeadlessHardCheck(
                HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                "orders_update:REJECTED_BY_SAFETY",
            ),
        )

        val passingChecks = HeadlessHardCheckEvaluator.evaluate(declinedWrite, checks)
        val failingChecks = HeadlessHardCheckEvaluator.evaluate(completedWrite, checks)

        assertThat(passingChecks).allMatch { it.passed }
        assertThat(failingChecks).allMatch { !it.passed }
    }

    private fun runResult(
        assistantText: String = "Assistant text",
        outcome: LoopOutcome = LoopOutcome.COMPLETED,
        toolCalls: List<HeadlessToolCallTrace> = emptyList(),
        confirmationResults: List<HeadlessConfirmationResultTrace> = emptyList(),
    ) = HeadlessRunResult(
        scenarioId = "scenario",
        turns = listOf(
            HeadlessTurnResult(
                turnIndex = 0,
                userMessage = "User message",
                assistantText = assistantText,
                outcome = outcome,
                toolCalls = toolCalls,
                confirmationResults = confirmationResults,
            )
        ),
    )

    private fun toolCall(
        name: String,
        resultKind: HeadlessToolResultKind = HeadlessToolResultKind.SUCCESS,
        arguments: kotlinx.serialization.json.JsonObject = buildJsonObject { },
    ) = HeadlessToolCallTrace(
        id = "$name-call",
        name = name,
        arguments = arguments,
        safetyLevel = ToolSafetyLevel.SAFE,
        resultKind = resultKind,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
