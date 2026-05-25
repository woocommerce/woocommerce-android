package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSampleClassification
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult

internal object WooAiSmokeSummaryRenderer {
    fun render(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison?,
    ): String = buildString {
        appendLine("# Woo AI Smoke Summary")
        appendLine("Model: ${suite.metadata.modelId}")
        appendLine("Prompt: ${suite.metadata.promptVersion}")
        appendLine("Tool catalog: ${suite.metadata.toolCatalogVersion}")
        appendLine("ChatService: ${suite.metadata.chatServiceClass}")
        appendLine("AuthProvider: ${suite.metadata.authProviderClass}")
        appendLine("ToolRegistry: ${suite.metadata.toolRegistryClass}")
        appendLine("Safety: ${suite.metadata.safetyPolicy}")
        appendLine("Store label: ${suite.metadata.smokeStoreLabel}")
        appendLine("Credential source: ${suite.metadata.credentialSource}")
        if (suite.metadata.sampleCount > 1) {
            appendLine("Sample count: ${suite.metadata.sampleCount}")
            appendLine(
                "Sampled mode: primary scenario status uses sample 1. Baselines may approve sampled PASS/FLAKY " +
                    "expectations."
            )
        }
        if (suite.metadata.scenarioFilter.isNotEmpty()) {
            appendLine("Scenario filter: ${suite.metadata.scenarioFilter.joinToString()}")
        }
        appendLine(statusCounts(suite, comparison))
        suite.scenarios.forEach { scenario ->
            appendLine()
            appendLine("## ${scenario.scenarioId}")
            appendLine("Status: ${scenario.status}")
            scenario.sampleSummary?.let { summary ->
                appendLine(
                    "Sampled classification: ${summary.classification} " +
                        "(PASS=${summary.passCount} FAIL=${summary.failCount})"
                )
            }
            appendLine("Tools: ${scenario.result.turns.flatMap { it.toolCalls }.toSummary()}")
            scenario.result.turns.flatMap { it.errors }
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { appendLine("Errors: ${it.joinToString()}") }
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
        comparison: HeadlessBaselineComparison?,
    ): String {
        val passCount = suite.scenarios.count { it.status == HeadlessScenarioStatus.PASS }
        val failCount = suite.scenarios.count { it.status == HeadlessScenarioStatus.FAIL }
        val sampledCounts = suite.scenarios.mapNotNull { it.sampleSummary?.classification }
            .groupingBy { it }
            .eachCount()
        if (comparison == null) {
            return "Status counts: PASS=$passCount FAIL=$failCount" + sampledCounts.toSummarySuffix()
        }

        val baselineCounts = comparison.scenarioStatuses.groupingBy { it.status }.eachCount()
        return "Status counts: PASS=$passCount FAIL=$failCount " +
            "KNOWN_FAILURE=${baselineCounts[HeadlessBaselineRegressionStatus.KNOWN_FAILURE] ?: 0} " +
            "KNOWN_FAILURE_FIXED=${baselineCounts[HeadlessBaselineRegressionStatus.KNOWN_FAILURE_FIXED] ?: 0} " +
            "NEW=${baselineCounts[HeadlessBaselineRegressionStatus.NEW] ?: 0} " +
            "MISSING=${baselineCounts[HeadlessBaselineRegressionStatus.MISSING] ?: 0} " +
            "REGRESSION=${baselineCounts[HeadlessBaselineRegressionStatus.REGRESSION] ?: 0}" +
            sampledCounts.toSummarySuffix()
    }

    private fun Map<HeadlessSampleClassification, Int>.toSummarySuffix(): String =
        if (isEmpty()) {
            ""
        } else {
            " SAMPLED_PASS=${this[HeadlessSampleClassification.PASS] ?: 0}" +
                " SAMPLED_FLAKY=${this[HeadlessSampleClassification.FLAKY] ?: 0}" +
                " SAMPLED_FAIL=${this[HeadlessSampleClassification.FAIL] ?: 0}"
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
