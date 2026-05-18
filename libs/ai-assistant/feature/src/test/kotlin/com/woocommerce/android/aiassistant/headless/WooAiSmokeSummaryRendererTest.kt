package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadataStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheck
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolCallTrace
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolResultKind
import com.woocommerce.android.aiassistant.core.headless.HeadlessTurnResult
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeSummaryRendererTest {
    @Test
    fun `when rendering summary, then metadata tool traces snippets and failed checks are visible`() {
        val summary = WooAiSmokeSummaryRenderer.render(
            suite = suite(),
            comparison = comparison(),
        )

        assertThat(summary).contains("# Woo AI Smoke Summary")
        assertThat(summary).contains("Model: gpt-4o")
        assertThat(summary).contains("Prompt: 1.0.0")
        assertThat(summary).contains("Tool catalog: 1.0.0")
        assertThat(summary).contains("ChatService: JetpackAiChatService")
        assertThat(summary).contains("ToolRegistry: WooCommerceToolRegistry")
        assertThat(summary).contains("Safety: ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)")
        assertThat(summary).contains(
            "Status counts: PASS=0 FAIL=1 KNOWN_FAILURE=0 KNOWN_FAILURE_FIXED=0 " +
                "NEW=1 MISSING=1 REGRESSION=1"
        )
        assertThat(summary).contains("Tools: orders_update(REJECTED_BY_SAFETY)")
        assertThat(summary).contains("Assistant: I can only help with your WooCommerce store.")
        assertThat(summary).contains("Failed checks:")
        assertThat(summary).contains("- Failed OUTCOME_EQUALS for COMPLETED")
    }

    private fun suite() = HeadlessSuiteRunResult(
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
                scenarioId = "write-confirmation-declined",
                category = "write",
                result = HeadlessRunResult(
                    scenarioId = "write-confirmation-declined",
                    turns = listOf(
                        HeadlessTurnResult(
                            turnIndex = 0,
                            userMessage = "Update order",
                            assistantText = "I can only help with your WooCommerce store.\nThe write was declined.",
                            outcome = LoopOutcome.STOPPED,
                            toolCalls = listOf(
                                HeadlessToolCallTrace(
                                    id = "call_1",
                                    name = "orders_update",
                                    arguments = buildJsonObject { },
                                    safetyLevel = ToolSafetyLevel.UNSAFE,
                                    resultKind = HeadlessToolResultKind.REJECTED_BY_SAFETY,
                                )
                            ),
                        )
                    ),
                ),
                hardCheckResults = listOf(
                    HeadlessHardCheckResult(
                        check = HeadlessHardCheck(HeadlessHardCheckType.OUTCOME_EQUALS, "COMPLETED"),
                        passed = false,
                        message = "Failed OUTCOME_EQUALS for COMPLETED",
                    )
                ),
                status = HeadlessScenarioStatus.FAIL,
            )
        ),
    )

    private fun comparison() = HeadlessBaselineComparison(
        metadataStatus = HeadlessBaselineMetadataStatus.CURRENT,
        scenarioStatuses = listOf(
            HeadlessBaselineScenarioStatus(
                scenarioId = "new-scenario",
                status = HeadlessBaselineRegressionStatus.NEW,
                message = "new",
            ),
            HeadlessBaselineScenarioStatus(
                scenarioId = "missing-scenario",
                status = HeadlessBaselineRegressionStatus.MISSING,
                message = "missing",
            ),
            HeadlessBaselineScenarioStatus(
                scenarioId = "write-confirmation-declined",
                status = HeadlessBaselineRegressionStatus.REGRESSION,
                message = "regression",
            ),
        ),
        message = "Scenario baseline failures.",
    )
}
