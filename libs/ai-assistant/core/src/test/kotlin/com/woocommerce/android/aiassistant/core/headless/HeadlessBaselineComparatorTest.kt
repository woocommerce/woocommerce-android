package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HeadlessBaselineComparatorTest {
    @Test
    fun `given metadata changed, when comparing, then status is stale`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4.1", scenario = passingScenario("orders-read-recent")),
            baseline = approvedBaseline(modelId = "gpt-4o", scenarioId = "orders-read-recent"),
        )

        assertThat(comparison.metadataStatus).isEqualTo(HeadlessBaselineMetadataStatus.STALE)
        assertThat(comparison.hasBlockingFailure).isTrue
    }

    @Test
    fun `given new current scenario, when comparing, then scenario is new`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = passingScenario("new-scenario")),
            baseline = approvedBaseline(modelId = "gpt-4o", scenarioId = "orders-read-recent"),
        )

        assertThat(comparison.scenarioStatuses.single { it.scenarioId == "new-scenario" }.status)
            .isEqualTo(HeadlessBaselineRegressionStatus.NEW)
    }

    @Test
    fun `given approved scenario fails, when comparing, then scenario is regression`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = failingScenario("orders-read-recent")),
            baseline = approvedBaseline(modelId = "gpt-4o", scenarioId = "orders-read-recent"),
        )

        assertThat(comparison.scenarioStatuses.single().status)
            .isEqualTo(HeadlessBaselineRegressionStatus.REGRESSION)
    }

    @Test
    fun `given known failure fails in expected shape, when comparing, then scenario is non blocking`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = failingScenario("orders-with-email")),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-with-email",
                knownFailure = knownFailure(),
            ),
        )

        assertThat(comparison.scenarioStatuses.single().status)
            .isEqualTo(HeadlessBaselineRegressionStatus.KNOWN_FAILURE)
        assertThat(comparison.hasBlockingFailure).isFalse
    }

    @Test
    fun `given known failure starts passing, when comparing, then scenario is marked fixed and non blocking`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = passingScenario("orders-with-email")),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-with-email",
                knownFailure = knownFailure(),
            ),
        )

        assertThat(comparison.scenarioStatuses.single().status)
            .isEqualTo(HeadlessBaselineRegressionStatus.KNOWN_FAILURE_FIXED)
        assertThat(comparison.hasBlockingFailure).isFalse
    }

    @Test
    fun `given approved scenario missing from current run, when comparing, then scenario is missing`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = passingScenario("products-search-card")),
            baseline = approvedBaseline(modelId = "gpt-4o", scenarioId = "orders-read-recent"),
        )

        assertThat(comparison.scenarioStatuses.single { it.scenarioId == "orders-read-recent" }.status)
            .isEqualTo(HeadlessBaselineRegressionStatus.MISSING)
    }

    private fun suite(
        modelId: String,
        scenario: HeadlessScenarioRunResult,
    ) = HeadlessSuiteRunResult(
        metadata = HeadlessRunMetadata(
            modelId = modelId,
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
        scenarios = listOf(scenario),
    )

    private fun passingScenario(scenarioId: String) = scenario(
        scenarioId = scenarioId,
        status = HeadlessScenarioStatus.PASS,
        hardCheckPassed = true,
    )

    private fun failingScenario(scenarioId: String) = scenario(
        scenarioId = scenarioId,
        status = HeadlessScenarioStatus.FAIL,
        hardCheckPassed = false,
    )

    private fun scenario(
        scenarioId: String,
        status: HeadlessScenarioStatus,
        hardCheckPassed: Boolean,
    ) = HeadlessScenarioRunResult(
        scenarioId = scenarioId,
        category = HeadlessScenarioCategory.ORDERS_READ,
        result = HeadlessRunResult(
            scenarioId = scenarioId,
            turns = listOf(
                HeadlessTurnResult(
                    turnIndex = 0,
                    userMessage = "Show orders",
                    assistantText = "Orders",
                    outcome = LoopOutcome.COMPLETED,
                    toolCalls = emptyList(),
                )
            ),
        ),
        hardCheckResults = listOf(
            HeadlessHardCheckResult(
                check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                passed = hardCheckPassed,
                message = "check",
            )
        ),
        status = status,
    )

    private fun approvedBaseline(
        modelId: String,
        scenarioId: String,
        knownFailure: HeadlessKnownFailure? = null,
    ) = HeadlessApprovedBaseline(
        version = 1,
        metadata = HeadlessBaselineMetadata(
            modelId = modelId,
            promptVersion = "1.0.0",
            toolCatalogVersion = "1.0.0",
        ),
        scenarios = listOf(
            HeadlessApprovedScenarioBaseline(
                scenarioId = scenarioId,
                category = HeadlessScenarioCategory.ORDERS_READ,
                approvedHardChecks = listOf(
                    HeadlessApprovedHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
                ),
                knownFailure = knownFailure,
            )
        ),
    )

    private fun knownFailure() = HeadlessKnownFailure(
        reason = "Model does not consistently mention where to find customer email.",
        issue = "WOOMOB-2922",
        expectedFailedHardChecks = listOf(
            HeadlessApprovedHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
        ),
    )
}
