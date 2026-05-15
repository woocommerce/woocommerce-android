package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.headless.HeadlessApprovedBaseline
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparator
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineComparison
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineParser
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
) {
    suspend fun run(): WooAiSmokeRunExit {
        val scenarioMapper = WooAiSmokeScenarioMapper(
            toolRegistry = toolRegistry,
            toolCatalogSelector = toolCatalogSelector,
            systemPromptProvider = systemPromptProvider,
            json = json,
            selectedSiteId = selectedSiteId,
        )
        val scenarioSpecs = scenarioMapper.loadScenarioSpecs()
        val suite = runSuite(scenarioSpecs, scenarioMapper)
        val baseline = loadApprovedBaseline()
        val comparison = HeadlessBaselineComparator.compare(suite, baseline)
        val approvedBaseline = if (config.baselineMode == WooAiSmokeBaselineMode.APPROVE) {
            WooAiSmokeBaselineApproval.approvedBaselineOrNull(suite)
        } else {
            null
        }
        val artifacts = WooAiSmokeRunWriter(json, outputDirectory).write(
            suite = suite,
            comparison = comparison,
            approvedBaseline = approvedBaseline,
        )
        return WooAiSmokeRunExit(
            artifactsDirectory = artifacts.outputDirectory,
            failureMessage = failureMessageFor(suite, comparison, approvedBaseline),
        )
    }

    private suspend fun runSuite(
        scenarioSpecs: List<HeadlessScenarioSpec>,
        scenarioMapper: WooAiSmokeScenarioMapper,
    ): HeadlessSuiteRunResult {
        val harness = createHarness()
        val scenarioResults = scenarioSpecs.map { spec ->
            val result = harness.runScenario(scenarioMapper.toHeadlessScenario(spec))
            val checksToEvaluate = spec.turns.flatMap { it.hardChecks } + spec.hardChecks
            val hardCheckResults = HeadlessHardCheckEvaluator.evaluate(
                result = result,
                checks = checksToEvaluate,
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
            toolRegistryClass = toolRegistry::class.simpleName ?: toolRegistry.javaClass.simpleName,
            safetyPolicy = "ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)",
        )

    private fun loadApprovedBaseline(): HeadlessApprovedBaseline {
        val source = requireNotNull(
            javaClass.classLoader?.getResource("woo-ai-smoke/baseline.json")
        ) { "Missing woo-ai-smoke/baseline.json" }.readText()
        return HeadlessBaselineParser(json).parseApprovedBaseline(source)
    }

    private fun failureMessageFor(
        suite: HeadlessSuiteRunResult,
        comparison: HeadlessBaselineComparison,
        approvedBaseline: HeadlessApprovedBaseline?,
    ): String? {
        val failedScenarios = suite.scenarios.filter { it.status == HeadlessScenarioStatus.FAIL }
        if (failedScenarios.isNotEmpty()) {
            return "Woo AI smoke hard checks failed: ${failedScenarios.joinToString { it.scenarioId }}"
        }

        return when (config.baselineMode) {
            WooAiSmokeBaselineMode.CHECK -> {
                if (comparison.hasBlockingFailure) {
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
