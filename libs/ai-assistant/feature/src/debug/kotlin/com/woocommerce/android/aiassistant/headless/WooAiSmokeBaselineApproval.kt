package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedScenarioBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult

internal object WooAiSmokeBaselineApproval {
    fun approvedBaselineOrNull(
        current: HeadlessSuiteRunResult,
        previousBaseline: HeadlessApprovedBaseline? = null,
    ): HeadlessApprovedBaseline? {
        val previousScenariosById = previousBaseline?.scenarios.orEmpty().associateBy { it.scenarioId }
        val unapprovedFailures = current.scenarios.filter { scenario ->
            scenario.isFailing() &&
                previousScenariosById[scenario.scenarioId]?.knownFailure == null
        }
        if (unapprovedFailures.isNotEmpty()) return null

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
                        scenario.isFailing()
                    },
                )
            },
        )
    }

    private fun HeadlessScenarioRunResult.isFailing(): Boolean =
        status == HeadlessScenarioStatus.FAIL
}
