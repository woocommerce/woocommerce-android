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
        return sampleCountMismatchStatus(expectation, currentSummary)
            ?: sampleExpectationStatus(approved, expectation, currentSummary)
    }

    private fun HeadlessScenarioRunResult.sampleCountMismatchStatus(
        expectation: HeadlessApprovedSampleExpectation,
        currentSummary: HeadlessScenarioSampleSummary?,
    ): HeadlessBaselineScenarioStatus? =
        if (currentSummary != null && currentSummary.requestedSamples != expectation.sampleCount) {
            HeadlessBaselineScenarioStatus(
                scenarioId = scenarioId,
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "Approved sample count is ${expectation.sampleCount}, current sample count is " +
                    "${currentSummary.requestedSamples}.",
            )
        } else {
            null
        }

    private fun HeadlessScenarioRunResult.sampleExpectationStatus(
        approved: HeadlessApprovedScenarioBaseline,
        expectation: HeadlessApprovedSampleExpectation,
        currentSummary: HeadlessScenarioSampleSummary?,
    ): HeadlessBaselineScenarioStatus? {
        val currentClassification = currentSampleClassification(currentSummary)
        return when (expectation.acceptedClassification) {
            HeadlessSampleClassification.PASS -> passSampleExpectationStatus(expectation, currentClassification)
            HeadlessSampleClassification.FLAKY -> flakySampleExpectationStatus(
                approved = approved,
                expectation = expectation,
                currentSummary = currentSummary,
                currentClassification = currentClassification,
            )
            HeadlessSampleClassification.FAIL -> sampleRegressionStatus(expectation, currentClassification)
        }
    }

    private fun HeadlessScenarioRunResult.passSampleExpectationStatus(
        expectation: HeadlessApprovedSampleExpectation,
        currentClassification: HeadlessSampleClassification,
    ): HeadlessBaselineScenarioStatus? =
        if (currentClassification == HeadlessSampleClassification.PASS) {
            null
        } else {
            sampleRegressionStatus(expectation, currentClassification)
        }

    private fun HeadlessScenarioRunResult.flakySampleExpectationStatus(
        approved: HeadlessApprovedScenarioBaseline,
        expectation: HeadlessApprovedSampleExpectation,
        currentSummary: HeadlessScenarioSampleSummary?,
        currentClassification: HeadlessSampleClassification,
    ): HeadlessBaselineScenarioStatus =
        when {
            currentClassification == HeadlessSampleClassification.FLAKY && sampleResults.isEmpty() ->
                HeadlessBaselineScenarioStatus(
                    scenarioId = scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "Approved flaky sample expectation requires sampled run details.",
                )
            currentClassification == HeadlessSampleClassification.FLAKY && hasSampledGlobalGuardFailures() ->
                HeadlessBaselineScenarioStatus(
                    scenarioId = scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "Approved flaky sample expectation has global guard failures.",
                )
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
                        message = "Approved flaky sample expectation now passes; refresh the baseline to accept PASS.",
                    )
            currentSummary == null && currentClassification == HeadlessSampleClassification.FAIL ->
                HeadlessBaselineScenarioStatus(
                    scenarioId = scenarioId,
                    status = HeadlessBaselineRegressionStatus.REGRESSION,
                    message = "Approved flaky sample expectation requires a sampled run; current single-sample " +
                        "status is FAIL.",
                )
            else -> sampleRegressionStatus(expectation, currentClassification)
        }

    private fun HeadlessScenarioRunResult.currentSampleClassification(
        currentSummary: HeadlessScenarioSampleSummary?,
    ): HeadlessSampleClassification =
        currentSummary?.classification ?: when (status) {
            HeadlessScenarioStatus.PASS -> HeadlessSampleClassification.PASS
            HeadlessScenarioStatus.FAIL -> HeadlessSampleClassification.FAIL
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

    private fun HeadlessScenarioRunResult.hasSampledGlobalGuardFailures(): Boolean =
        sampleResults.any { sample ->
            sample.hardCheckResults.any { hardCheckResult ->
                hardCheckResult.check.type in GLOBAL_GUARD_TYPES && !hardCheckResult.passed
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

private val GLOBAL_GUARD_TYPES = setOf(
    HeadlessHardCheckType.NO_FAILED_OUTCOME,
    HeadlessHardCheckType.NO_TURN_ERRORS,
    HeadlessHardCheckType.ASSISTANT_TEXT_NOT_BLANK,
)

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
