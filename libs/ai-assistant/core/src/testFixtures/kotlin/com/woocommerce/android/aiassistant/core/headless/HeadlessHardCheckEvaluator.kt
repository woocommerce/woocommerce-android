package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
data class HeadlessHardCheckResult(
    val check: HeadlessHardCheck,
    val passed: Boolean,
    val message: String,
)

object HeadlessHardCheckEvaluator {
    fun evaluate(
        result: HeadlessRunResult,
        scenario: HeadlessScenarioSpec,
    ): List<HeadlessHardCheckResult> {
        val turnResultsByIndex = result.turns.associateBy { it.turnIndex }
        val turnCheckResults = scenario.turns.flatMapIndexed { index, turnSpec ->
            val turnResult = turnResultsByIndex[index]
            if (turnResult == null) {
                turnSpec.hardChecks.map { check ->
                    HeadlessHardCheckResult(
                        check = check,
                        passed = false,
                        message = "Failed ${check.type} for ${check.value}: turn $index did not run",
                    )
                }
            } else {
                evaluate(
                    result = result.copy(turns = listOf(turnResult)),
                    checks = turnSpec.hardChecks,
                )
            }
        }
        return turnCheckResults + evaluate(result, scenario.hardChecks)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun evaluate(
        result: HeadlessRunResult,
        checks: List<HeadlessHardCheck>,
    ): List<HeadlessHardCheckResult> = checks.map { check ->
        val combinedAssistantText = result.turns.joinToString("\n") { it.assistantText }
        val toolCalls = result.turns.flatMap { it.toolCalls }
        val confirmationResults = result.turns.flatMap { it.confirmationResults }
        val passed = when (check.type) {
            HeadlessHardCheckType.OUTCOME_EQUALS ->
                result.turns.all { it.outcome.name == check.value }
            HeadlessHardCheckType.ASSISTANT_TEXT_CONTAINS ->
                combinedAssistantText.contains(check.value, ignoreCase = true)
            HeadlessHardCheckType.ASSISTANT_TEXT_NOT_CONTAINS ->
                !combinedAssistantText.contains(check.value, ignoreCase = true)
            HeadlessHardCheckType.ASSISTANT_TEXT_CONTAINS_ANY ->
                parsePipeSeparatedValues(check.value).any {
                    combinedAssistantText.contains(it, ignoreCase = true)
                }
            HeadlessHardCheckType.ASSISTANT_REFUSAL ->
                check.value == "woocommerce_scope" && combinedAssistantText.isWooCommerceScopeRefusal()
            HeadlessHardCheckType.TOOL_CALLED ->
                toolCalls.any { it.name == check.value }
            HeadlessHardCheckType.TOOL_CALLED_ANY ->
                parsePipeSeparatedValues(check.value).any { expectedTool ->
                    toolCalls.any { it.name == expectedTool }
                }
            HeadlessHardCheckType.TOOL_NOT_CALLED ->
                toolCalls.none { it.name == check.value }
            HeadlessHardCheckType.TOOL_CALL_COUNT_AT_MOST -> {
                val expectation = parseToolCountLimit(check.value)
                toolCalls.count { it.name == expectation.toolName } <= expectation.maxCount
            }
            HeadlessHardCheckType.TOTAL_TOOL_CALL_COUNT_AT_MOST ->
                toolCalls.size <= check.value.toInt()
            HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS -> {
                val expectation = parseToolResultExpectation(check.value)
                val namedToolCalls = toolCalls.filter { it.name == expectation.toolName }
                namedToolCalls.isNotEmpty() &&
                    namedToolCalls.all { it.resultKind.name == expectation.resultKind }
            }
            HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS ->
                confirmationResults.isNotEmpty() && confirmationResults.all { it.decision == check.value }
            HeadlessHardCheckType.TOOL_ARGUMENT_JSON_CONTAINS -> {
                val expectation = parseToolArgumentExpectation(check.value, Json)
                toolCalls.any {
                    it.name == expectation.toolName && it.arguments.containsSubset(expectation.expectedArguments)
                }
            }
            HeadlessHardCheckType.TOOL_ARGUMENT_NOT_CONTAINS -> {
                val expectation = parseToolArgumentTextExpectation(check.value)
                toolCalls
                    .filter { it.name == expectation.toolName }
                    .none { it.arguments.toString().contains(expectation.forbiddenText, ignoreCase = true) }
            }
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

    private fun parsePipeSeparatedValues(value: String): List<String> =
        value.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    private fun parseToolCountLimit(value: String): ToolCountLimit {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2) { "Expected <toolName>:<maxCount> for TOOL_CALL_COUNT_AT_MOST" }
        return ToolCountLimit(
            toolName = parts[0],
            maxCount = parts[1].toInt(),
        )
    }

    private fun parseToolResultExpectation(value: String): ToolResultExpectation {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2) { "Expected <toolName>:<resultKind> for TOOL_RESULT_KIND_EQUALS" }
        return ToolResultExpectation(
            toolName = parts[0],
            resultKind = parts[1],
        )
    }

    private fun parseToolArgumentExpectation(value: String, json: Json): ToolArgumentExpectation {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2) { "Expected <toolName>:<jsonObjectSubset> for TOOL_ARGUMENT_JSON_CONTAINS" }
        return ToolArgumentExpectation(
            toolName = parts[0],
            expectedArguments = json.parseToJsonElement(parts[1]).jsonObject,
        )
    }

    private fun parseToolArgumentTextExpectation(value: String): ToolArgumentTextExpectation {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2) { "Expected <toolName>:<forbiddenText> for TOOL_ARGUMENT_NOT_CONTAINS" }
        return ToolArgumentTextExpectation(
            toolName = parts[0],
            forbiddenText = parts[1],
        )
    }

    private fun JsonElement.containsSubset(expected: JsonElement): Boolean = when {
        this is JsonObject && expected is JsonObject ->
            expected.all { (key, expectedValue) ->
                this[key]?.containsSubset(expectedValue) == true
            }
        this is JsonArray && expected is JsonArray -> {
            val unmatchedActualValues = toMutableList()
            expected.all { expectedValue ->
                val actualIndex = unmatchedActualValues.indexOfFirst { actualValue ->
                    actualValue.containsSubset(expectedValue)
                }
                if (actualIndex == -1) {
                    false
                } else {
                    unmatchedActualValues.removeAt(actualIndex)
                    true
                }
            }
        }
        else -> this == expected
    }

    private fun String.isWooCommerceScopeRefusal(): Boolean {
        val refusalTokens = listOf(
            "can't",
            "cannot",
            "can help",
            "can only",
            "not able",
            "outside",
            "here to help",
            "assist with",
            "focus on",
        )
        val scopeTokens = listOf("woocommerce", "store")
        return refusalTokens.any { contains(it, ignoreCase = true) } &&
            scopeTokens.any { contains(it, ignoreCase = true) }
    }

    private data class ToolCountLimit(
        val toolName: String,
        val maxCount: Int,
    )

    private data class ToolResultExpectation(
        val toolName: String,
        val resultKind: String,
    )

    private data class ToolArgumentExpectation(
        val toolName: String,
        val expectedArguments: JsonObject,
    )

    private data class ToolArgumentTextExpectation(
        val toolName: String,
        val forbiddenText: String,
    )
}
