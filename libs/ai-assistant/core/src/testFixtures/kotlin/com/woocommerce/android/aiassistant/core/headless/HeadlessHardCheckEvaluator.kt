package com.woocommerce.android.aiassistant.core.headless

data class HeadlessHardCheckResult(
    val check: HeadlessHardCheck,
    val passed: Boolean,
    val message: String,
)

object HeadlessHardCheckEvaluator {
    fun evaluate(
        result: HeadlessRunResult,
        checks: List<HeadlessHardCheck>,
    ): List<HeadlessHardCheckResult> = checks.map { check ->
        val passed = when (check.type) {
            HeadlessHardCheckType.OUTCOME_EQUALS ->
                result.turns.all { it.outcome.name == check.value }
            HeadlessHardCheckType.ASSISTANT_TEXT_CONTAINS ->
                result.turns.any { it.assistantText.contains(check.value) }
            HeadlessHardCheckType.TOOL_CALLED ->
                result.turns.any { turn -> turn.toolCalls.any { it.name == check.value } }
            HeadlessHardCheckType.TOOL_NOT_CALLED ->
                result.turns.none { turn -> turn.toolCalls.any { it.name == check.value } }
        }
        HeadlessHardCheckResult(
            check = check,
            passed = passed,
            message = if (passed) {
                "Passed ${check.type} for ${check.value}"
            } else {
                "Failed ${check.type} for ${check.value}"
            },
        )
    }
}
