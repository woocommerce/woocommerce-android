package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedSampleExpectation
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedScenarioBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessKnownFailure
import com.woocommerce.android.aiassistant.core.headless.HeadlessSampleClassification
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult

internal object WooAiSmokeBaselineApproval {
    fun approvedBaselineOrNull(
        current: HeadlessSuiteRunResult,
        previousBaseline: HeadlessApprovedBaseline? = null,
    ): HeadlessApprovedBaseline? {
        val previousScenariosById = previousBaseline?.scenarios.orEmpty().associateBy { it.scenarioId }
        val unapprovedScenarios = current.scenarios.filter { scenario ->
            !scenario.isApprovable(
                sampleCount = current.metadata.sampleCount,
                previousScenario = previousScenariosById[scenario.scenarioId],
            )
        }
        if (unapprovedScenarios.isNotEmpty()) return null

        return HeadlessApprovedBaseline(
            metadata = HeadlessBaselineMetadata(
                modelId = current.metadata.modelId,
                promptVersion = current.metadata.promptVersion,
                toolCatalogVersion = current.metadata.toolCatalogVersion,
            ),
            scenarios = current.scenarios.map { scenario ->
                val previousScenario = previousScenariosById[scenario.scenarioId]
                HeadlessApprovedScenarioBaseline(
                    scenarioId = scenario.scenarioId,
                    category = scenario.category,
                    approvedHardChecks = scenario.hardCheckResults.map { it.check },
                    knownFailure = previousScenario?.knownFailure?.takeIf {
                        scenario.matchesKnownFailure(it)
                    },
                    sampleExpectation = scenario.sampleExpectationOrNull(current.metadata.sampleCount),
                )
            },
        )
    }

    private fun HeadlessScenarioRunResult.isApprovable(
        sampleCount: Int,
        previousScenario: HeadlessApprovedScenarioBaseline?,
    ): Boolean {
        if (sampleCount == 1) {
            return !isFailing() || matchesKnownFailure(previousScenario?.knownFailure)
        }

        val summary = sampleSummary ?: return false
        return when (summary.classification) {
            HeadlessSampleClassification.PASS,
            HeadlessSampleClassification.FLAKY -> true
            HeadlessSampleClassification.FAIL -> matchesKnownFailure(previousScenario?.knownFailure)
        }
    }

    private fun HeadlessScenarioRunResult.sampleExpectationOrNull(
        sampleCount: Int,
    ): HeadlessApprovedSampleExpectation? {
        if (sampleCount == 1) return null

        val summary = sampleSummary ?: return null
        return when (summary.classification) {
            HeadlessSampleClassification.PASS,
            HeadlessSampleClassification.FLAKY -> HeadlessApprovedSampleExpectation(
                sampleCount = summary.requestedSamples,
                acceptedClassification = summary.classification,
            )
            HeadlessSampleClassification.FAIL -> null
        }
    }

    private fun HeadlessScenarioRunResult.isFailing(): Boolean =
        status == HeadlessScenarioStatus.FAIL

    private fun HeadlessScenarioRunResult.matchesKnownFailure(
        knownFailure: HeadlessKnownFailure?,
    ): Boolean {
        if (!isFailing() || knownFailure == null) return false

        return failedHardChecks() == knownFailure.expectedFailedHardChecks.toSet()
    }

    private fun HeadlessScenarioRunResult.failedHardChecks() =
        hardCheckResults
            .filterNot { it.passed }
            .map { it.check }
            .toSet()
}
