package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedScenarioBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessKnownFailure
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessSampleClassification
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioSampleRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioSampleSummary
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessTurnResult
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeBaselineApprovalTest {
    @Test
    fun `given failed run, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(scenario(status = HeadlessScenarioStatus.FAIL))
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given passing run, when generating approval, then metadata scenarios and categories are copied`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(
                scenario(
                    scenarioId = "orders-read-recent",
                    category = "read",
                ),
                scenario(
                    scenarioId = "write-confirmation-declined",
                    category = "write",
                    hardCheck = HeadlessHardCheck(
                        HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                        "orders_update:REJECTED_BY_SAFETY",
                    ),
                ),
            )
        )

        requireNotNull(approval)
        assertThat(approval.metadata.modelId).isEqualTo("gpt-4o")
        assertThat(approval.metadata.promptVersion).isEqualTo("1.0.0")
        assertThat(approval.metadata.toolCatalogVersion).isEqualTo("1.0.0")
        assertThat(approval.scenarios.map { it.scenarioId })
            .containsExactly("orders-read-recent", "write-confirmation-declined")
        assertThat(approval.scenarios.map { it.category })
            .containsExactly("read", "write")
        assertThat(approval.scenarios.last().approvedHardChecks.single().value)
            .isEqualTo("orders_update:REJECTED_BY_SAFETY")
    }

    @Test
    fun `given failed known failure, when generating approval, then known failure is preserved`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                )
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        requireNotNull(approval)
        val scenario = approval.scenarios.single()
        assertThat(scenario.knownFailure?.reason)
            .isEqualTo("Model does not consistently mention where to find customer email.")
    }

    @Test
    fun `given known failure has extra failed hard check, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                    hardCheckResults = listOf(
                        hardCheckResult(
                            check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                            passed = false,
                        ),
                        hardCheckResult(
                            check = HeadlessHardCheck(
                                HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                                "orders_search:SUCCESS",
                            ),
                            passed = false,
                        ),
                    ),
                )
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given known failure has different failed hard check, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                    hardCheckResults = listOf(
                        hardCheckResult(
                            check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                            passed = true,
                        ),
                        hardCheckResult(
                            check = HeadlessHardCheck(
                                HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                                "orders_search:SUCCESS",
                            ),
                            passed = false,
                        ),
                    ),
                )
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given known failure starts passing, when generating approval, then known failure is cleared`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.PASS,
                )
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        requireNotNull(approval)
        val scenario = approval.scenarios.single()
        assertThat(scenario.knownFailure).isNull()
    }

    @Test
    fun `given all-pass sampled run, when generating approval, then pass sample expectation is written`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(
                scenario(
                    sampleSummary = sampleSummary(HeadlessSampleClassification.PASS),
                ),
                sampleCount = 3,
            )
        )

        requireNotNull(approval)
        assertThat(approval.scenarios.single().sampleExpectation?.sampleCount).isEqualTo(3)
        assertThat(approval.scenarios.single().sampleExpectation?.acceptedClassification)
            .isEqualTo(HeadlessSampleClassification.PASS)
    }

    @Test
    fun `given mixed sampled run, when generating approval, then flaky sample expectation is written`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(
                scenario(
                    sampleSummary = sampleSummary(HeadlessSampleClassification.FLAKY),
                ),
                sampleCount = 3,
            )
        )

        requireNotNull(approval)
        assertThat(approval.scenarios.single().sampleExpectation?.sampleCount).isEqualTo(3)
        assertThat(approval.scenarios.single().sampleExpectation?.acceptedClassification)
            .isEqualTo(HeadlessSampleClassification.FLAKY)
    }

    @Test
    fun `given all-fail sampled run, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(
                scenario(
                    status = HeadlessScenarioStatus.FAIL,
                    sampleSummary = sampleSummary(HeadlessSampleClassification.FAIL),
                ),
                sampleCount = 3,
            )
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given sampled approval without sample summary, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            suite(
                scenario(),
                sampleCount = 3,
            )
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given all-fail sampled known failure, when generating approval, then known failure is preserved`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                    sampleSummary = sampleSummary(HeadlessSampleClassification.FAIL),
                ),
                sampleCount = 3,
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        requireNotNull(approval)
        val scenario = approval.scenarios.single()
        assertThat(scenario.knownFailure?.reason)
            .isEqualTo("Model does not consistently mention where to find customer email.")
        assertThat(scenario.sampleExpectation).isNull()
    }

    @Test
    fun `given sampled known failure has later changed failure shape, when generating approval, then null is returned`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                    hardCheckResults = expectedFailureHardCheckResults(),
                    sampleResults = listOf(
                        sampleResult(
                            sampleIndex = 1,
                            hardCheckResults = expectedFailureHardCheckResults(),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                        sampleResult(
                            sampleIndex = 2,
                            hardCheckResults = expectedFailureHardCheckResults() + hardCheckResult(
                                check = HeadlessHardCheck(
                                    HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
                                    "orders_search:SUCCESS",
                                ),
                                passed = false,
                            ),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                        sampleResult(
                            sampleIndex = 3,
                            hardCheckResults = expectedFailureHardCheckResults(),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                    ),
                    sampleSummary = sampleSummary(HeadlessSampleClassification.FAIL),
                ),
                sampleCount = 3,
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        assertThat(approval).isNull()
    }

    @Test
    fun `given sampled known failure has matching failing samples, when generating approval, then known failure is preserved`() {
        val approval = WooAiSmokeBaselineApproval.approvedBaselineOrNull(
            current = suite(
                scenario(
                    scenarioId = "orders-with-email",
                    status = HeadlessScenarioStatus.FAIL,
                    hardCheckResults = expectedFailureHardCheckResults(),
                    sampleResults = listOf(
                        sampleResult(
                            sampleIndex = 1,
                            hardCheckResults = expectedFailureHardCheckResults(),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                        sampleResult(
                            sampleIndex = 2,
                            hardCheckResults = expectedFailureHardCheckResults(),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                        sampleResult(
                            sampleIndex = 3,
                            hardCheckResults = expectedFailureHardCheckResults(),
                            status = HeadlessScenarioStatus.FAIL,
                        ),
                    ),
                    sampleSummary = sampleSummary(HeadlessSampleClassification.FAIL),
                ),
                sampleCount = 3,
            ),
            previousBaseline = previousBaselineWithKnownFailure(),
        )

        requireNotNull(approval)
        val scenario = approval.scenarios.single()
        assertThat(scenario.knownFailure?.reason)
            .isEqualTo("Model does not consistently mention where to find customer email.")
        assertThat(scenario.sampleExpectation).isNull()
    }

    private fun suite(
        vararg scenarios: HeadlessScenarioRunResult,
        sampleCount: Int = 1,
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
            sampleCount = sampleCount,
        ),
        scenarios = scenarios.toList(),
    )

    private fun scenario(
        scenarioId: String = "scenario",
        category: String = "read",
        status: HeadlessScenarioStatus = HeadlessScenarioStatus.PASS,
        hardCheck: HeadlessHardCheck = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
        hardCheckResults: List<HeadlessHardCheckResult> = listOf(
            hardCheckResult(
                check = hardCheck,
                passed = status == HeadlessScenarioStatus.PASS,
            )
        ),
        sampleResults: List<HeadlessScenarioSampleRunResult> = emptyList(),
        sampleSummary: HeadlessScenarioSampleSummary? = null,
    ) = HeadlessScenarioRunResult(
        scenarioId = scenarioId,
        category = category,
        result = runResult(scenarioId),
        hardCheckResults = hardCheckResults,
        status = status,
        sampleResults = sampleResults,
        sampleSummary = sampleSummary,
    )

    private fun sampleResult(
        sampleIndex: Int,
        hardCheckResults: List<HeadlessHardCheckResult>,
        status: HeadlessScenarioStatus,
    ) = HeadlessScenarioSampleRunResult(
        sampleIndex = sampleIndex,
        result = runResult("scenario"),
        hardCheckResults = hardCheckResults,
        status = status,
    )

    private fun runResult(
        scenarioId: String,
    ) = HeadlessRunResult(
        scenarioId = scenarioId,
        turns = listOf(
            HeadlessTurnResult(
                turnIndex = 0,
                userMessage = "User",
                assistantText = "Assistant",
                outcome = LoopOutcome.COMPLETED,
                toolCalls = emptyList(),
            )
        ),
    )

    private fun sampleSummary(
        classification: HeadlessSampleClassification,
    ) = HeadlessScenarioSampleSummary(
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

    private fun hardCheckResult(
        check: HeadlessHardCheck,
        passed: Boolean,
    ) = HeadlessHardCheckResult(
        check = check,
        passed = passed,
        message = "check",
    )

    private fun expectedFailureHardCheckResults() = listOf(
        hardCheckResult(
            check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
            passed = false,
        )
    )

    private fun previousBaselineWithKnownFailure() = HeadlessApprovedBaseline(
        metadata = HeadlessBaselineMetadata(
            modelId = "gpt-4o",
            promptVersion = "1.0.0",
            toolCatalogVersion = "1.0.0",
        ),
        scenarios = listOf(
            HeadlessApprovedScenarioBaseline(
                scenarioId = "orders-with-email",
                category = "read",
                approvedHardChecks = listOf(
                    HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
                ),
                knownFailure = HeadlessKnownFailure(
                    reason = "Model does not consistently mention where to find customer email.",
                    expectedFailedHardChecks = listOf(
                        HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED")
                    ),
                ),
            )
        ),
    )
}
