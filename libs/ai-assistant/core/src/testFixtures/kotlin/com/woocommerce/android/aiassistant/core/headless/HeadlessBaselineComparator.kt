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
                approved.knownFailure != null -> scenario.knownFailureStatus(approved)
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

    private fun HeadlessScenarioRunResult.knownFailureStatus(
        approved: HeadlessApprovedScenarioBaseline,
    ): HeadlessBaselineScenarioStatus {
        val knownFailure = requireNotNull(approved.knownFailure)
        if (status == HeadlessScenarioStatus.PASS) {
            return HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.KNOWN_FAILURE_FIXED,
                message = "Known failure now passes; remove knownFailure and refresh the baseline.",
            )
        }

        val currentChecks = hardCheckResults.associateBy { it.check.type to it.check.value }
        val expectedFailedChecks = knownFailure.expectedFailedHardChecks.toSet()
        val actualFailedChecks = hardCheckResults
            .filterNot { it.passed }
            .map { it.check }
            .toSet()
        val expectedFailuresStillFail = expectedFailedChecks.all { expectedFailed ->
            currentChecks[expectedFailed.type to expectedFailed.value]?.passed == false
        }
        val onlyExpectedFailuresFail = actualFailedChecks == expectedFailedChecks
        val requiredChecksStillPass = approved.approvedHardChecks
            .filterNot { it in expectedFailedChecks }
            .all { approvedCheck ->
                currentChecks[approvedCheck.type to approvedCheck.value]?.passed == true
            }

        return if (expectedFailuresStillFail && onlyExpectedFailuresFail && requiredChecksStillPass) {
            HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.KNOWN_FAILURE,
                message = "Known failure accepted: ${knownFailure.reason}",
            )
        } else {
            HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "Known failure shape changed.",
            )
        }
    }

    private fun comparisonMessage(
        metadataStatus: HeadlessBaselineMetadataStatus,
        scenarioStatuses: List<HeadlessBaselineScenarioStatus>,
    ): String {
        val blocking = scenarioStatuses.filter { it.status.isBlocking }
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
            scenarioStatuses.any { it.status.isBlocking }
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
    KNOWN_FAILURE,
    KNOWN_FAILURE_FIXED,
    NEW,
    MISSING,
    REGRESSION,
}

private val HeadlessBaselineRegressionStatus.isBlocking: Boolean
    get() = when (this) {
        HeadlessBaselineRegressionStatus.PASS,
        HeadlessBaselineRegressionStatus.KNOWN_FAILURE,
        HeadlessBaselineRegressionStatus.KNOWN_FAILURE_FIXED -> false
        HeadlessBaselineRegressionStatus.NEW,
        HeadlessBaselineRegressionStatus.MISSING,
        HeadlessBaselineRegressionStatus.REGRESSION -> true
    }
