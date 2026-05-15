package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedScenarioBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult

internal object WooAiSmokeBaselineApproval {
    fun approvedBaselineOrNull(current: HeadlessSuiteRunResult): HeadlessApprovedBaseline? {
        if (current.scenarios.any { it.status != HeadlessScenarioStatus.PASS }) return null
        return HeadlessApprovedBaseline(
            version = 1,
            metadata = HeadlessBaselineMetadata(
                modelId = current.metadata.modelId,
                promptVersion = current.metadata.promptVersion,
                toolCatalogVersion = current.metadata.toolCatalogVersion,
            ),
            scenarios = current.scenarios.map { scenario ->
                HeadlessApprovedScenarioBaseline(
                    scenarioId = scenario.scenarioId,
                    category = scenario.category,
                    approvedStatus = HeadlessScenarioStatus.PASS,
                    approvedHardChecks = scenario.hardCheckResults.map {
                        HeadlessApprovedHardCheck(it.check.type, it.check.value)
                    },
                )
            },
        )
    }
}
