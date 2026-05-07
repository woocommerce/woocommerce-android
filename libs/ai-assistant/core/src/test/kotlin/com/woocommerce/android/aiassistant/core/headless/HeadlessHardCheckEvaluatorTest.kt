package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.json.buildJsonObject
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
}
