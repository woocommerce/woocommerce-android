package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolCallTrace
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

internal class WooAiSmokeRunWriter(
    private val json: Json,
    private val outputDirectory: File,
    private val approvedBaselineFileName: String?,
    private val redactor: WooAiSmokeRedactor,
    private val usePerRunDirectory: Boolean,
) {
    fun write(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison?,
        approvedBaseline: HeadlessApprovedBaseline?,
    ): WooAiSmokeArtifacts {
        val sourceOutputDirectory = if (usePerRunDirectory) perRunDirectory() else outputDirectory
        sourceOutputDirectory.deleteRecursively()
        sourceOutputDirectory.mkdirs()
        File(sourceOutputDirectory, "run.json").writeRedacted(json.encodeToString(suite))
        if (comparison != null) {
            File(sourceOutputDirectory, "baseline-comparison.json").writeRedacted(json.encodeToString(comparison))
        }
        File(sourceOutputDirectory, "summary.md").writeRedacted(WooAiSmokeSummaryRenderer.render(suite, comparison))
        File(
            sourceOutputDirectory,
            "turns.jsonl",
        ).writeRedacted(
            turnRecords(suite).joinToString("\n") { json.encodeToString(it) }
        )
        if (approvedBaseline != null) {
            val fileName = requireNotNull(approvedBaselineFileName) {
                "Approved baseline file name is required when writing an approved baseline"
            }
            File(sourceOutputDirectory, fileName).writeRedacted(json.encodeToString(approvedBaseline))
        }
        if (usePerRunDirectory) {
            outputDirectory.deleteRecursively()
            sourceOutputDirectory.copyRecursively(outputDirectory, overwrite = true)
        }
        return WooAiSmokeArtifacts(
            outputDirectory = outputDirectory,
            sourceOutputDirectory = sourceOutputDirectory,
        )
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

    private fun File.writeRedacted(text: String) {
        writeText(redactor.redact(text))
    }

    private fun perRunDirectory(): File {
        val latestParent = requireNotNull(outputDirectory.parentFile) {
            "Output directory must have a parent: $outputDirectory"
        }
        val timestamp = RUN_DIRECTORY_TIMESTAMP.format(Instant.now())
        val shortRunId = UUID.randomUUID().toString().take(SHORT_RUN_ID_LENGTH)
        return File(latestParent, "runs/$timestamp-$shortRunId")
    }

    private companion object {
        private const val SHORT_RUN_ID_LENGTH = 8
        private val RUN_DIRECTORY_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
    }
}

internal data class WooAiSmokeArtifacts(
    val outputDirectory: File,
    val sourceOutputDirectory: File,
) {
    fun artifactDirectories(): List<File> = listOf(sourceOutputDirectory, outputDirectory)
        .distinctBy { it.absolutePath }
}

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
