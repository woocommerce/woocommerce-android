package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WooAiSmokeRobolectricTest {
    private val json = WooAiSmokeNoDeviceHarness.json

    @Test
    fun `when primary no-device smoke runs, then scenarios baseline and stable artifacts pass`() = runTest {
        val outputDirectory = WooAiSmokeNoDeviceHarness.stableOutputDirectory()

        val exit = WooAiSmokeNoDeviceHarness.runner(
            outputDirectory = outputDirectory,
            baselineMode = WooAiSmokeBaselineMode.CHECK,
        ).run()

        assertThat(exit.failureMessage).isNull()
        assertThat(exit.artifactsDirectory).isEqualTo(outputDirectory)
        assertRequiredArtifacts(outputDirectory)
        assertThat(File(outputDirectory, "approved-baseline.json")).doesNotExist()

        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val comparison = json.decodeFromString<HeadlessBaselineComparison>(
            File(outputDirectory, "baseline-comparison.json").readText()
        )
        val summary = File(outputDirectory, "summary.md").readText()
        val turns = File(outputDirectory, "turns.jsonl").readLines()

        assertThat(suite.metadata.modelId).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(suite.metadata.promptVersion).isEqualTo(AssistantConfig.PROMPT_VERSION)
        assertThat(suite.metadata.toolCatalogVersion).isEqualTo(AssistantConfig.TOOL_CATALOG_VERSION)
        assertThat(suite.metadata.chatServiceClass).isEqualTo("WooAiSmokeNoDeviceChatService")
        assertThat(suite.metadata.toolRegistryClass).isEqualTo("WooAiSmokeNoDeviceToolRegistry")
        assertThat(suite.scenarios.map { it.scenarioId }).containsExactly(
            "orders-read-recent",
            "products-search-card",
            "analytics-orders-this-month",
            "write-confirmation-declined",
            "off-domain-refusal",
        )
        assertThat(suite.scenarios).allMatch { it.status == HeadlessScenarioStatus.PASS }
        assertThat(comparison.scenarioStatuses).allMatch {
            it.status == HeadlessBaselineRegressionStatus.PASS
        }
        assertThat(turns).hasSize(5)
        assertThat(summary).contains("Status counts: PASS=5 FAIL=0 NEW=0 MISSING=0 REGRESSION=0")
        assertThat(summary).contains("Safety: ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)")
    }

    private fun assertRequiredArtifacts(outputDirectory: File) {
        assertThat(File(outputDirectory, "run.json")).exists()
        assertThat(File(outputDirectory, "turns.jsonl")).exists()
        assertThat(File(outputDirectory, "summary.md")).exists()
        assertThat(File(outputDirectory, "baseline-comparison.json")).exists()
    }
}
