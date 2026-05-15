package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult

internal object WooAiSmokeSummaryRenderer {
    fun render(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison,
    ): String = buildString {
        appendLine("# Woo AI Smoke Summary")
        appendLine("Model: ${suite.metadata.modelId}")
        appendLine("Prompt: ${suite.metadata.promptVersion}")
        appendLine("Tool catalog: ${suite.metadata.toolCatalogVersion}")
        appendLine("ChatService: ${suite.metadata.chatServiceClass}")
        appendLine("ToolRegistry: ${suite.metadata.toolRegistryClass}")
        appendLine("Safety: ${suite.metadata.safetyPolicy}")
        appendLine(statusCounts(suite, comparison))
        suite.scenarios.forEach { scenario ->
            appendLine()
            appendLine("## ${scenario.scenarioId}")
            appendLine("Status: ${scenario.status}")
            appendLine("Tools: ${scenario.result.turns.flatMap { it.toolCalls }.toSummary()}")
            appendLine("Assistant: ${scenario.result.turns.joinToString(" ") { it.assistantText }.snippet()}")
            val failedChecks = scenario.hardCheckResults.filterNot { it.passed }
            if (failedChecks.isNotEmpty()) {
                appendLine("Failed checks:")
                failedChecks.forEach { appendLine("- ${it.message}") }
            }
        }
    }

    private fun statusCounts(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison,
    ): String {
        val passCount = suite.scenarios.count { it.status == HeadlessScenarioStatus.PASS }
        val failCount = suite.scenarios.count { it.status == HeadlessScenarioStatus.FAIL }
        val baselineCounts = comparison.scenarioStatuses.groupingBy { it.status }.eachCount()
        return "Status counts: PASS=$passCount FAIL=$failCount " +
            "NEW=${baselineCounts[HeadlessBaselineRegressionStatus.NEW] ?: 0} " +
            "MISSING=${baselineCounts[HeadlessBaselineRegressionStatus.MISSING] ?: 0} " +
            "REGRESSION=${baselineCounts[HeadlessBaselineRegressionStatus.REGRESSION] ?: 0}"
    }

    private fun List<com.woocommerce.android.aiassistant.core.headless.HeadlessToolCallTrace>.toSummary(): String =
        if (isEmpty()) {
            "none"
        } else {
            joinToString { "${it.name}(${it.resultKind})" }
        }

    private fun String.snippet(): String =
        replace(Regex("\\s+"), " ").trim().take(MAX_ASSISTANT_SNIPPET_CHARS)

    private const val MAX_ASSISTANT_SNIPPET_CHARS = 500
}
