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
    @Suppress("CyclomaticComplexMethod")
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
            HeadlessHardCheckType.ASSISTANT_REFUSAL ->
                check.value == "woocommerce_scope" && combinedAssistantText.isWooCommerceScopeRefusal()
            HeadlessHardCheckType.TOOL_CALLED ->
                toolCalls.any { it.name == check.value }
            HeadlessHardCheckType.TOOL_NOT_CALLED ->
                toolCalls.none { it.name == check.value }
            HeadlessHardCheckType.TOOL_CALL_COUNT_AT_MOST -> {
                val expectation = parseToolCountLimit(check.value)
                toolCalls.count { it.name == expectation.toolName } <= expectation.maxCount
            }
            HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS -> {
                val expectation = parseToolResultExpectation(check.value)
                toolCalls.any {
                    it.name == expectation.toolName && it.resultKind.name == expectation.resultKind
                }
            }
            HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS ->
                confirmationResults.any { it.decision == check.value }
            HeadlessHardCheckType.TOOL_ARGUMENT_JSON_CONTAINS -> {
                val expectation = parseToolArgumentExpectation(check.value, Json)
                toolCalls.any {
                    it.name == expectation.toolName && it.arguments.containsSubset(expectation.expectedArguments)
                }
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

    private fun JsonElement.containsSubset(expected: JsonElement): Boolean = when {
        this is JsonObject && expected is JsonObject ->
            expected.all { (key, expectedValue) ->
                this[key]?.containsSubset(expectedValue) == true
            }
        this is JsonArray && expected is JsonArray ->
            size == expected.size && zip(expected).all { (actualValue, expectedValue) ->
                actualValue.containsSubset(expectedValue)
            }
        else -> this == expected
    }

    private fun String.isWooCommerceScopeRefusal(): Boolean {
        val refusalTokens = listOf("can't", "cannot", "can only", "not able", "outside")
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
}
