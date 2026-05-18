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
                approved.sampleExpectation != null -> scenario.sampleExpectationStatusOrNull(approved)
                    ?: scenario.standardStatus(approved)
                else -> scenario.standardStatus(approved)
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

    private fun HeadlessScenarioRunResult.standardStatus(
        approved: HeadlessApprovedScenarioBaseline,
    ): HeadlessBaselineScenarioStatus =
        when {
            status != HeadlessScenarioStatus.PASS -> HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "Scenario status is $status.",
            )
            !hasApprovedChecksPassing(approved) -> HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "One or more approved hard checks are absent or failing.",
            )
            else -> HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.PASS,
                message = "Scenario matches approved baseline.",
            )
        }

    private fun HeadlessScenarioRunResult.sampleExpectationStatusOrNull(
        approved: HeadlessApprovedScenarioBaseline,
    ): HeadlessBaselineScenarioStatus? {
        val expectation = requireNotNull(approved.sampleExpectation)
        val currentSummary = sampleSummary
        if (currentSummary != null && currentSummary.requestedSamples != expectation.sampleCount) {
            return HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "Approved sample count is ${expectation.sampleCount}, current sample count is " +
                    "${currentSummary.requestedSamples}.",
            )
        }

        val currentClassification = currentSummary?.classification ?: when (status) {
            HeadlessScenarioStatus.PASS -> HeadlessSampleClassification.PASS
            HeadlessScenarioStatus.FAIL -> HeadlessSampleClassification.FAIL
        }

        return when (expectation.acceptedClassification) {
            HeadlessSampleClassification.PASS -> if (currentClassification == HeadlessSampleClassification.PASS) {
                null
            } else {
                sampleRegressionStatus(expectation, currentClassification)
            }
            HeadlessSampleClassification.FLAKY -> when {
                currentClassification == HeadlessSampleClassification.FLAKY -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenarioId,
                    status = HeadlessBaselineRegressionStatus.PASS,
                    message = "Approved flaky sample expectation still matches.",
                )
                currentClassification == HeadlessSampleClassification.PASS ->
                    standardStatus(approved).takeUnless { it.status == HeadlessBaselineRegressionStatus.PASS }
                        ?: HeadlessBaselineScenarioStatus(
                            scenarioId = scenarioId,
                            status = HeadlessBaselineRegressionStatus.PASS,
                            message = "Approved flaky sample expectation now passes; refresh the baseline to " +
                                "accept PASS.",
                        )
                currentSummary == null -> HeadlessBaselineScenarioStatus(
                    scenarioId = scenarioId,
                    status = HeadlessBaselineRegressionStatus.PASS,
                    message = "Single-sample FAIL accepted by approved flaky sample expectation; run sampled check " +
                        "to confirm before refreshing the baseline.",
                )
                else -> sampleRegressionStatus(expectation, currentClassification)
            }
            HeadlessSampleClassification.FAIL -> sampleRegressionStatus(expectation, currentClassification)
        }
    }

    private fun HeadlessScenarioRunResult.sampleRegressionStatus(
        expectation: HeadlessApprovedSampleExpectation,
        currentClassification: HeadlessSampleClassification,
    ) = HeadlessBaselineScenarioStatus(
        scenarioId = scenarioId,
        status = HeadlessBaselineRegressionStatus.REGRESSION,
        message = "Approved sample classification is ${expectation.acceptedClassification}, current sample " +
            "classification is $currentClassification.",
    )

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
        val failingSamples = failingSampleHardCheckResults()
        if (failingSamples.isEmpty()) {
            return HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.KNOWN_FAILURE_FIXED,
                message = "Known failure now passes; remove knownFailure and refresh the baseline.",
            )
        }

        val expectedFailedChecks = knownFailure.expectedFailedHardChecks.toSet()
        val failingSamplesMatchKnownFailure = failingSamples.all { hardCheckResults ->
            hardCheckResults.failedHardChecks() == expectedFailedChecks
        }
        val requiredChecksStillPass = failingSamples.all { hardCheckResults ->
            val currentChecks = hardCheckResults.associateBy { it.check.type to it.check.value }
            approved.approvedHardChecks
                .filterNot { it in expectedFailedChecks }
                .all { approvedCheck ->
                    currentChecks[approvedCheck.type to approvedCheck.value]?.passed == true
                }
        }

        return if (failingSamplesMatchKnownFailure && requiredChecksStillPass) {
            HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.KNOWN_FAILURE,
                message = "Known failure accepted: ${knownFailure.reason}",
            )
        } else {
            HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "Known failure sampled failure shape changed.",
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
