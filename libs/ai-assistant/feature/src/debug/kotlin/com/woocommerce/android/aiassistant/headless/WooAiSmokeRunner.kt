@file:Suppress("ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparator
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineMetadataStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineParser
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineRegressionStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckEvaluator
import com.woocommerce.android.aiassistant.core.headless.HeadlessRunMetadata
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioRunResult
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioSpec
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import com.woocommerce.android.aiassistant.core.headless.ScriptedHeadlessSafetyOrchestrator
import com.woocommerce.android.aiassistant.core.headless.WooAssistantHeadless
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlin.time.TimeSource

@Suppress("LongParameterList")
internal class WooAiSmokeRunner(
    private val chatService: ChatService,
    private val toolRegistry: ToolRegistry,
    private val toolCatalogSelector: ToolCatalogSelector,
    private val retryPolicy: RetryPolicy,
    private val historyBudgeter: HistoryBudgeter,
    private val systemPromptProvider: AssistantSystemPromptProvider,
    private val json: Json,
    private val timeSource: TimeSource,
    private val config: WooAiSmokeConfig,
    private val selectedSiteId: Long,
    private val outputDirectory: File,
    private val jwtProviderClass: String,
    private val storeLabel: String,
    private val credentialSource: String,
    private val redactor: WooAiSmokeRedactor,
) {
    suspend fun run(): WooAiSmokeRunExit {
        val scenarioMapper = WooAiSmokeScenarioMapper(
            toolRegistry = toolRegistry,
            toolCatalogSelector = toolCatalogSelector,
            systemPromptProvider = systemPromptProvider,
            json = json,
            selectedSiteId = selectedSiteId,
            resourceName = config.scenarioResourceName,
        )
        val scenarioSpecs = scenarioMapper.loadScenarioSpecs()
        val suite = runSuite(scenarioSpecs, scenarioMapper)
        val baseline = loadApprovedBaselineOrNull()
        val comparison = baselineComparisonOrNull(suite, baseline)
        val approvedBaseline = if (config.baseline?.mode == WooAiSmokeBaselineMode.APPROVE) {
            WooAiSmokeBaselineApproval.approvedBaselineOrNull(
                current = suite,
                previousBaseline = baseline,
            )
        } else {
            null
        }
        val artifacts = WooAiSmokeRunWriter(
            json = json,
            outputDirectory = outputDirectory,
            approvedBaselineFileName = config.baseline?.approvedFileName,
            redactor = redactor,
            usePerRunDirectory = config.usePerRunDirectory,
        ).write(
            suite = suite,
            comparison = comparison,
            approvedBaseline = approvedBaseline,
        )
        return WooAiSmokeRunExit(
            artifactsDirectory = artifacts.outputDirectory,
            sourceArtifactsDirectory = artifacts.sourceOutputDirectory,
            failureMessage = failureMessageFor(
                suite = suite,
                comparison = comparison,
                approvedBaseline = approvedBaseline,
                baselineMissing = baseline == null,
            ),
        )
    }

    private suspend fun runSuite(
        scenarioSpecs: List<HeadlessScenarioSpec>,
        scenarioMapper: WooAiSmokeScenarioMapper,
    ): HeadlessSuiteRunResult {
        val harness = createHarness()
        val scenarioResults = scenarioSpecs.map { spec ->
            val result = harness.runScenario(scenarioMapper.toHeadlessScenario(spec))
            val hardCheckResults = HeadlessHardCheckEvaluator.evaluate(
                result = result,
                scenario = spec,
            )
            HeadlessScenarioRunResult(
                scenarioId = spec.id,
                category = spec.category,
                result = result,
                hardCheckResults = hardCheckResults,
                status = if (hardCheckResults.all { it.passed }) {
                    HeadlessScenarioStatus.PASS
                } else {
                    HeadlessScenarioStatus.FAIL
                },
            )
        }
        return HeadlessSuiteRunResult(
            metadata = currentMetadata(),
            scenarios = scenarioResults,
        )
    }

    private fun createHarness(): WooAssistantHeadless {
        val safety = ScriptedHeadlessSafetyOrchestrator(
            defaultDecision = ConfirmationDecision.CANCELLED,
        )
        return WooAssistantHeadless(
            chatService = chatService,
            toolRegistry = toolRegistry,
            retryPolicy = retryPolicy,
            historyBudgeter = historyBudgeter,
            json = json,
            timeSource = timeSource,
            safetyOrchestrator = safety,
        )
    }

    private fun currentMetadata(): HeadlessRunMetadata =
        HeadlessRunMetadata(
            modelId = AssistantConfig.MODEL_ID,
            promptVersion = AssistantConfig.PROMPT_VERSION,
            toolCatalogVersion = AssistantConfig.TOOL_CATALOG_VERSION,
            startedAtIso8601 = Instant.now().toString(),
            chatServiceClass = chatService::class.simpleName ?: chatService.javaClass.simpleName,
            jwtProviderClass = jwtProviderClass,
            toolRegistryClass = toolRegistry::class.simpleName ?: toolRegistry.javaClass.simpleName,
            safetyPolicy = "ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)",
            smokeStoreLabel = storeLabel,
            credentialSource = credentialSource,
        )

    private fun loadApprovedBaselineOrNull(): HeadlessApprovedBaseline? =
        config.baseline?.resourceName
            ?.let { javaClass.classLoader?.getResource("woo-ai-smoke/$it") }
            ?.readText()
            ?.let { HeadlessBaselineParser(json).parseApprovedBaseline(it) }

    private fun baselineComparisonOrNull(
        suite: HeadlessSuiteRunResult,
        baseline: HeadlessApprovedBaseline?,
    ): HeadlessBaselineComparison? {
        val baselineConfig = config.baseline ?: return null
        return if (baseline != null) {
            HeadlessBaselineComparator.compare(suite, baseline)
        } else {
            missingBaselineComparison(suite, baselineConfig)
        }
    }

    private fun missingBaselineComparison(
        suite: HeadlessSuiteRunResult,
        baselineConfig: WooAiSmokeBaselineConfig,
    ) = HeadlessBaselineComparison(
        metadataStatus = HeadlessBaselineMetadataStatus.STALE,
        scenarioStatuses = suite.scenarios.map { scenario ->
            HeadlessBaselineScenarioStatus(
                scenarioId = scenario.scenarioId,
                status = HeadlessBaselineRegressionStatus.NEW,
                message = "Scenario has no approved live baseline.",
            )
        },
        message = "Live baseline approval required: missing woo-ai-smoke/${baselineConfig.resourceName}",
    )

    private fun failureMessageFor(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison?,
        approvedBaseline: HeadlessApprovedBaseline?,
        baselineMissing: Boolean,
    ): String? {
        val baselineConfig = config.baseline
            ?: return suite.scenarios
                .filter { it.status == HeadlessScenarioStatus.FAIL }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(
                    prefix = "Woo AI deterministic smoke failed: ",
                    transform = { it.scenarioId },
                )

        return when (baselineConfig.mode) {
            WooAiSmokeBaselineMode.CHECK -> {
                if (baselineMissing) {
                    "Live baseline approval required: missing woo-ai-smoke/${baselineConfig.resourceName}"
                } else if (comparison?.hasBlockingFailure == true) {
                    "Woo AI smoke baseline check failed: ${comparison.message}"
                } else {
                    null
                }
            }
            WooAiSmokeBaselineMode.APPROVE -> {
                if (approvedBaseline == null) {
                    "Woo AI smoke approval did not produce an approved baseline"
                } else {
                    null
                }
            }
        }
    }
}
