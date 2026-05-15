package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioCategory
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
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
                    category = HeadlessScenarioCategory.ORDERS_READ,
                ),
                scenario(
                    scenarioId = "write-confirmation-declined",
                    category = HeadlessScenarioCategory.WRITE_CONFIRMATION,
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
            .containsExactly(HeadlessScenarioCategory.ORDERS_READ, HeadlessScenarioCategory.WRITE_CONFIRMATION)
        assertThat(approval.scenarios.last().approvedHardChecks.single().value)
            .isEqualTo("orders_update:REJECTED_BY_SAFETY")
    }

    private fun suite(
        vararg scenarios: HeadlessScenarioRunResult,
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
        scenarios = scenarios.toList(),
    )

    private fun scenario(
        scenarioId: String = "scenario",
        category: HeadlessScenarioCategory = HeadlessScenarioCategory.ORDERS_READ,
        status: HeadlessScenarioStatus = HeadlessScenarioStatus.PASS,
        hardCheck: HeadlessHardCheck = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
    ) = HeadlessScenarioRunResult(
        scenarioId = scenarioId,
        category = category,
        result = HeadlessRunResult(
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
        ),
        hardCheckResults = listOf(
            HeadlessHardCheckResult(
                check = hardCheck,
                passed = status == HeadlessScenarioStatus.PASS,
                message = "check",
            )
        ),
        status = status,
    )
}
