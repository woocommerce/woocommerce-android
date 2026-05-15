package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WooAiSmokeRobolectricApprovalTest {
    private val json = WooAiSmokeNoDeviceHarness.json

    @Test
    fun `when no-device approval runs, then approved baseline is written to stable artifacts`() = runTest {
        val outputDirectory = WooAiSmokeNoDeviceHarness.stableOutputDirectory()

        val exit = WooAiSmokeNoDeviceHarness.runner(
            outputDirectory = outputDirectory,
            baselineMode = WooAiSmokeBaselineMode.APPROVE,
        ).run()

        assertThat(exit.failureMessage).isNull()
        assertThat(exit.artifactsDirectory).isEqualTo(outputDirectory)
        assertThat(File(outputDirectory, "run.json")).exists()
        assertThat(File(outputDirectory, "turns.jsonl")).exists()
        assertThat(File(outputDirectory, "summary.md")).exists()
        assertThat(File(outputDirectory, "baseline-comparison.json")).exists()

        val approvedBaselineFile = File(outputDirectory, "approved-baseline.json")
        assertThat(approvedBaselineFile).exists()

        val approvedBaseline = json.decodeFromString<HeadlessApprovedBaseline>(
            approvedBaselineFile.readText()
        )
        assertThat(approvedBaseline.metadata.modelId).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(approvedBaseline.metadata.promptVersion).isEqualTo(AssistantConfig.PROMPT_VERSION)
        assertThat(approvedBaseline.metadata.toolCatalogVersion).isEqualTo(AssistantConfig.TOOL_CATALOG_VERSION)
        assertThat(approvedBaseline.scenarios.map { it.scenarioId }).containsExactly(
            "orders-read-recent",
            "products-search-card",
            "analytics-orders-this-month",
            "write-confirmation-declined",
            "off-domain-refusal",
        )
        assertThat(approvedBaseline.scenarios).allMatch {
            it.approvedStatus == HeadlessScenarioStatus.PASS
        }
        assertThat(approvedBaseline.scenarios).allMatch {
            it.approvedHardChecks.isNotEmpty()
        }
    }
}
