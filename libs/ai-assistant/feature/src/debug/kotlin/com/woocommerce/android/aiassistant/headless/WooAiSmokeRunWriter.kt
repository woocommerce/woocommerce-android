package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolCallTrace
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal class WooAiSmokeRunWriter(
    private val json: Json,
    private val outputDirectory: File,
) {
    fun write(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison,
        approvedBaseline: HeadlessApprovedBaseline?,
    ): WooAiSmokeArtifacts {
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        File(outputDirectory, "run.json").writeText(json.encodeToString(suite))
        File(outputDirectory, "baseline-comparison.json").writeText(json.encodeToString(comparison))
        File(outputDirectory, "summary.md").writeText(WooAiSmokeSummaryRenderer.render(suite, comparison))
        File(outputDirectory, "turns.jsonl").writeText(turnRecords(suite).joinToString("\n") { json.encodeToString(it) })
        if (approvedBaseline != null) {
            File(outputDirectory, "approved-baseline.json").writeText(json.encodeToString(approvedBaseline))
        }
        return WooAiSmokeArtifacts(outputDirectory = outputDirectory)
    }

    private fun turnRecords(suite: HeadlessSuiteRunResult): List<WooAiSmokeTurnRecord> =
        suite.scenarios.flatMap { scenario ->
            scenario.result.turns.map { turn ->
                WooAiSmokeTurnRecord(
                    scenarioId = scenario.scenarioId,
                    turnIndex = turn.turnIndex,
                    userMessage = turn.userMessage,
                    assistantText = turn.assistantText,
                    outcome = turn.outcome.name,
                    toolCalls = turn.toolCalls,
                    errors = turn.errors,
                )
            }
        }
}

internal data class WooAiSmokeArtifacts(
    val outputDirectory: File,
)

@Serializable
private data class WooAiSmokeTurnRecord(
    val scenarioId: String,
    val turnIndex: Int,
    val userMessage: String,
    val assistantText: String,
    val outcome: String,
    val toolCalls: List<HeadlessToolCallTrace>,
    val errors: List<String>,
)
