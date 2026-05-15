package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.Serializable

object HeadlessBaselineComparator {
    fun compare(
        current: HeadlessSuiteRunResult,
        baseline: HeadlessApprovedBaseline,
    ): HeadlessBaselineComparison {
        val metadataStatus = if (
            current.metadata.modelId == baseline.metadata.modelId &&
            current.metadata.promptVersion == baseline.metadata.promptVersion &&
            current.metadata.toolCatalogVersion == baseline.metadata.toolCatalogVersion
        ) {
            HeadlessBaselineMetadataStatus.CURRENT
        } else {
            HeadlessBaselineMetadataStatus.STALE
        }
        val currentById = current.scenarios.associateBy { it.scenarioId }
        val baselineById = baseline.scenarios.associateBy { it.scenarioId }
        val currentStatuses = current.scenarios.map { scenario ->
            val approved = baselineById[scenario.scenarioId]
            when {
                approved == null -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenario.scenarioId,
                    status = HeadlessBaselineRegressionStatus.NEW,
                    message = "Scenario is not present in approved baseline.",
                )
                metadataStatus == HeadlessBaselineMetadataStatus.STALE -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenario.scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "Baseline metadata is stale.",
                )
                scenario.status != HeadlessScenarioStatus.PASS -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenario.scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "Scenario status is ${scenario.status}.",
                )
                !scenario.hasApprovedChecksPassing(approved) -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenario.scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "One or more approved hard checks are absent or failing.",
                )
                else -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenario.scenarioId,
                    status = HeadlessBaselineRegressionStatus.PASS,
                    message = "Scenario matches approved baseline.",
                )
            }
        }
        val missingStatuses = baseline.scenarios
            .filterNot { it.scenarioId in currentById }
            .map {
                HeadlessBaselineScenarioStatus(
                    scenarioId = it.scenarioId,
                    status = HeadlessBaselineRegressionStatus.MISSING,
                    message = "Approved scenario is missing from current run.",
                )
            }
        val scenarioStatuses = currentStatuses + missingStatuses
        return HeadlessBaselineComparison(
            metadataStatus = metadataStatus,
            scenarioStatuses = scenarioStatuses,
            message = comparisonMessage(metadataStatus, scenarioStatuses),
        )
    }

    private fun HeadlessScenarioRunResult.hasApprovedChecksPassing(
        approved: HeadlessApprovedScenarioBaseline,
    ): Boolean {
        val currentChecks = hardCheckResults.associateBy { it.check.type to it.check.value }
        return approved.approvedHardChecks.all { approvedCheck ->
            currentChecks[approvedCheck.type to approvedCheck.value]?.passed == true
        }
    }

    private fun comparisonMessage(
        metadataStatus: HeadlessBaselineMetadataStatus,
        scenarioStatuses: List<HeadlessBaselineScenarioStatus>,
    ): String {
        val blocking = scenarioStatuses.filter { it.status != HeadlessBaselineRegressionStatus.PASS }
        return when {
            metadataStatus == HeadlessBaselineMetadataStatus.STALE ->
                "Approved baseline metadata is stale."
            blocking.isNotEmpty() ->
                "Scenario baseline failures: ${blocking.joinToString { "${it.scenarioId}=${it.status}" }}"
            else -> "Approved baseline is current."
        }
    }
}

@Serializable
data class HeadlessBaselineComparison(
    val metadataStatus: HeadlessBaselineMetadataStatus,
    val scenarioStatuses: List<HeadlessBaselineScenarioStatus>,
    val message: String,
) {
    val hasBlockingFailure: Boolean
        get() = metadataStatus != HeadlessBaselineMetadataStatus.CURRENT ||
            scenarioStatuses.any { it.status != HeadlessBaselineRegressionStatus.PASS }
}

@Serializable
data class HeadlessBaselineScenarioStatus(
    val scenarioId: String,
    val status: HeadlessBaselineRegressionStatus,
    val message: String,
)

@Serializable
enum class HeadlessBaselineMetadataStatus {
    CURRENT,
    STALE,
}

@Serializable
enum class HeadlessBaselineRegressionStatus {
    PASS,
    NEW,
    MISSING,
    REGRESSION,
}
