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

    @Test
    fun `given approved pass sample expectation and current flaky, when comparing, then scenario is regression`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(
                modelId = "gpt-4o",
                scenario = passingScenario("orders-read-recent").withSampleSummary(HeadlessSampleClassification.FLAKY),
            ),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-read-recent",
                sampleExpectation = sampleExpectation(HeadlessSampleClassification.PASS),
            ),
        )

        val scenarioStatus = comparison.scenarioStatuses.single()
        assertThat(scenarioStatus.status).isEqualTo(HeadlessBaselineRegressionStatus.REGRESSION)
        assertThat(scenarioStatus.message).contains("sample classification")
    }

    @Test
    fun `given approved flaky sample expectation and current flaky, when comparing, then scenario is non blocking`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(
                modelId = "gpt-4o",
                scenario = passingScenario("orders-read-recent").withSampleSummary(HeadlessSampleClassification.FLAKY),
            ),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-read-recent",
                sampleExpectation = sampleExpectation(HeadlessSampleClassification.FLAKY),
            ),
        )

        val scenarioStatus = comparison.scenarioStatuses.single()
        assertThat(scenarioStatus.status).isEqualTo(HeadlessBaselineRegressionStatus.PASS)
        assertThat(scenarioStatus.message).contains("Approved flaky sample expectation still matches")
        assertThat(comparison.hasBlockingFailure).isFalse
    }

    @Test
    fun `given approved flaky sample expectation and current pass, when comparing, then scenario is non blocking`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = passingScenario("orders-read-recent")),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-read-recent",
                sampleExpectation = sampleExpectation(HeadlessSampleClassification.FLAKY),
            ),
        )

        val scenarioStatus = comparison.scenarioStatuses.single()
        assertThat(scenarioStatus.status).isEqualTo(HeadlessBaselineRegressionStatus.PASS)
        assertThat(scenarioStatus.message).contains("Approved flaky sample expectation now passes")
        assertThat(comparison.hasBlockingFailure).isFalse
    }

    @Test
    fun `given approved flaky sample expectation and sampled current fail, when comparing, then scenario is regression`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(
                modelId = "gpt-4o",
                scenario = failingScenario("orders-read-recent").withSampleSummary(HeadlessSampleClassification.FAIL),
            ),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-read-recent",
                sampleExpectation = sampleExpectation(HeadlessSampleClassification.FLAKY),
            ),
        )

        val scenarioStatus = comparison.scenarioStatuses.single()
        assertThat(scenarioStatus.status).isEqualTo(HeadlessBaselineRegressionStatus.REGRESSION)
        assertThat(scenarioStatus.message).contains("sample classification")
    }

    @Test
    fun `given approved flaky sample expectation and single-sample current fail, when comparing, then scenario is non blocking`() {
        val comparison = HeadlessBaselineComparator.compare(
            current = suite(modelId = "gpt-4o", scenario = failingScenario("orders-read-recent")),
            baseline = approvedBaseline(
                modelId = "gpt-4o",
                scenarioId = "orders-read-recent",
                sampleExpectation = sampleExpectation(HeadlessSampleClassification.FLAKY),
            ),
        )

        val scenarioStatus = comparison.scenarioStatuses.single()
        assertThat(scenarioStatus.status).isEqualTo(HeadlessBaselineRegressionStatus.PASS)
        assertThat(scenarioStatus.message).contains("Single-sample FAIL accepted by approved flaky sample expectation")
        assertThat(comparison.hasBlockingFailure).isFalse
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
        category = "read",
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

    private fun HeadlessScenarioRunResult.withSampleSummary(
        classification: HeadlessSampleClassification,
    ) = copy(
        sampleSummary = HeadlessScenarioSampleSummary(
            requestedSamples = 3,
            passCount = when (classification) {
                HeadlessSampleClassification.PASS -> 3
                HeadlessSampleClassification.FLAKY -> 2
                HeadlessSampleClassification.FAIL -> 0
            },
            failCount = when (classification) {
                HeadlessSampleClassification.PASS -> 0
                HeadlessSampleClassification.FLAKY -> 1
                HeadlessSampleClassification.FAIL -> 3
            },
            classification = classification,
        )
    )

    private fun approvedBaseline(
        modelId: String,
        scenarioId: String,
        knownFailure: HeadlessKnownFailure? = null,
        sampleExpectation: HeadlessApprovedSampleExpectation? = null,
    ) = HeadlessApprovedBaseline(
        metadata = HeadlessBaselineMetadata(
            modelId = modelId,
            promptVersion = "1.0.0",
            toolCatalogVersion = "1.0.0",
        ),
        scenarios = listOf(
            HeadlessApprovedScenarioBaseline(
                scenarioId = scenarioId,
                category = "read",
                approvedHardChecks = listOf(
                    HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
                ),
                knownFailure = knownFailure,
                sampleExpectation = sampleExpectation,
            )
        ),
    )

    private fun sampleExpectation(
        classification: HeadlessSampleClassification,
    ) = HeadlessApprovedSampleExpectation(
        sampleCount = 3,
        acceptedClassification = classification,
    )

    private fun knownFailure() = HeadlessKnownFailure(
        reason = "Model does not consistently mention where to find customer email.",
        expectedFailedHardChecks = listOf(
            HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
        ),
    )
}
