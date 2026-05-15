package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedScenarioBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadataStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioCategory
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolCallTrace
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolResultKind
import com.woocommerce.android.aiassistant.core.headless.HeadlessTurnResult
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WooAiSmokeRunWriterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `when writing run artifacts, then machine readable and summary files are created`() {
        val outputDirectory = temporaryFolder.newFolder("latest")
        val artifacts = writer(outputDirectory).write(
            suite = suite(),
            comparison = comparison(),
            approvedBaseline = approvedBaseline(),
        )

        assertThat(artifacts.outputDirectory).isEqualTo(outputDirectory)
        assertThat(outputDirectory.resolve("run.json")).exists()
        assertThat(outputDirectory.resolve("turns.jsonl")).exists()
        assertThat(outputDirectory.resolve("summary.md")).exists()
        assertThat(outputDirectory.resolve("baseline-comparison.json")).exists()
        assertThat(outputDirectory.resolve("approved-baseline.json")).exists()
        assertThat(outputDirectory.resolve("turns.jsonl").readLines()).hasSize(1)
        assertThat(outputDirectory.resolve("summary.md").readText()).contains("orders_list(SUCCESS)")
    }

    @Test
    fun `given no approved baseline, when writing run artifacts, then approval file is omitted`() {
        val outputDirectory = temporaryFolder.newFolder("latest")
        writer(outputDirectory).write(
            suite = suite(),
            comparison = comparison(),
            approvedBaseline = null,
        )

        assertThat(outputDirectory.resolve("approved-baseline.json")).doesNotExist()
    }

    @Test
    fun `given artifact text contains secrets, when writing run artifacts, then secrets are redacted`() {
        val outputDirectory = temporaryFolder.newFolder("latest")

        writer(outputDirectory).write(
            suite = suite(assistantText = "merchant@example.com app password"),
            comparison = comparison(),
            approvedBaseline = null,
        )

        assertThat(outputDirectory.walkTopDown().filter { it.isFile }.joinToString("\n") { it.readText() })
            .doesNotContain("merchant@example.com")
            .doesNotContain("app password")
    }

    @Test
    fun `given per run layout, when writing run artifacts, then latest is updated after a run directory is written`() {
        val latestDirectory = temporaryFolder.newFolder("live").resolve("latest")

        val artifacts = writer(latestDirectory, usePerRunDirectory = true).write(
            suite = suite(),
            comparison = comparison(),
            approvedBaseline = null,
        )

        assertThat(artifacts.outputDirectory).isEqualTo(latestDirectory)
        assertThat(latestDirectory.resolve("run.json")).exists()
        assertThat(requireNotNull(latestDirectory.parentFile).resolve("runs").listFiles()).hasSize(1)
    }

    private fun suite(
        assistantText: String = "Here are your recent orders.",
    ) = HeadlessSuiteRunResult(
        metadata = HeadlessRunMetadata(
            modelId = "gpt-4o",
            promptVersion = "1.0.0",
            toolCatalogVersion = "1.0.0",
            startedAtIso8601 = "2026-05-15T00:00:00Z",
            chatServiceClass = "JetpackAiChatService",
            jwtProviderClass = "WooAiSmokeDirectJwtTokenProvider",
            toolRegistryClass = "WooCommerceToolRegistry",
            safetyPolicy = "ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)",
            smokeStoreLabel = "store",
            credentialSource = "test",
        ),
        scenarios = listOf(
            HeadlessScenarioRunResult(
                scenarioId = "orders-read-recent",
                category = HeadlessScenarioCategory.ORDERS_READ,
                result = HeadlessRunResult(
                    scenarioId = "orders-read-recent",
                    turns = listOf(
                        HeadlessTurnResult(
                            turnIndex = 0,
                            userMessage = "Show orders",
                            assistantText = assistantText,
                            outcome = LoopOutcome.COMPLETED,
                            toolCalls = listOf(
                                HeadlessToolCallTrace(
                                    id = "call_1",
                                    name = "orders_list",
                                    arguments = buildJsonObject { },
                                    safetyLevel = ToolSafetyLevel.SAFE,
                                    resultKind = HeadlessToolResultKind.SUCCESS,
                                )
                            ),
                        )
                    ),
                ),
                hardCheckResults = listOf(
                    HeadlessHardCheckResult(
                        check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                        passed = true,
                        message = "Passed",
                    )
                ),
                status = HeadlessScenarioStatus.PASS,
            )
        ),
    )

    private fun comparison() = HeadlessBaselineComparison(
        metadataStatus = HeadlessBaselineMetadataStatus.CURRENT,
        scenarioStatuses = listOf(
            HeadlessBaselineScenarioStatus(
                scenarioId = "orders-read-recent",
                status = HeadlessBaselineRegressionStatus.PASS,
                message = "pass",
            )
        ),
        message = "Approved baseline is current.",
    )

    private fun approvedBaseline() = HeadlessApprovedBaseline(
        version = 1,
        metadata = HeadlessBaselineMetadata(
            modelId = "gpt-4o",
            promptVersion = "1.0.0",
            toolCatalogVersion = "1.0.0",
        ),
        scenarios = listOf(
            HeadlessApprovedScenarioBaseline(
                scenarioId = "orders-read-recent",
                category = HeadlessScenarioCategory.ORDERS_READ,
                approvedStatus = HeadlessScenarioStatus.PASS,
                approvedHardChecks = listOf(
                    HeadlessApprovedHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
                ),
            )
        ),
    )

    private fun writer(
        outputDirectory: File,
        usePerRunDirectory: Boolean = false,
    ) = WooAiSmokeRunWriter(
        json = json,
        outputDirectory = outputDirectory,
        approvedBaselineFileName = "approved-baseline.json",
        redactor = WooAiSmokeRedactor(
            siteUrl = "https://store.example",
            username = "merchant@example.com",
            appPassword = "app password",
        ),
        usePerRunDirectory = usePerRunDirectory,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
