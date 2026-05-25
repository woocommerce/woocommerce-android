package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessSampleClassification
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioStatus
import com.woocommerce.android.aiassistant.core.headless.HeadlessSuiteRunResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WooAiSmokeDeterministicSupportTest {
    private val json = WooAiSmokeDeterministicSupportFixtures.json

    @Test
    fun `when deterministic no-device smoke runs, then scenarios and stable artifacts pass`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(outputDirectory).run()

        assertThat(exit.failureMessage).isNull()
        assertThat(exit.artifactsDirectory).isEqualTo(outputDirectory)
        assertRequiredArtifacts(outputDirectory)

        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val summary = File(outputDirectory, "summary.md").readText()
        val turns = File(outputDirectory, "turns.jsonl").readLines()

        assertThat(suite.metadata.modelId).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(suite.metadata.promptVersion).isEqualTo(AssistantConfig.PROMPT_VERSION)
        assertThat(suite.metadata.toolCatalogVersion).isEqualTo(AssistantConfig.TOOL_CATALOG_VERSION)
        assertThat(suite.metadata.chatServiceClass).isEqualTo("WooAiSmokeDeterministicSupportChatService")
        assertThat(suite.metadata.toolRegistryClass).isEqualTo("WooAiSmokeDeterministicSupportToolRegistry")
        assertThat(suite.scenarios.map { it.scenarioId }).containsExactly(
            "orders-read-recent",
            "products-search-card",
            "analytics-orders-this-month",
            "write-confirmation-declined",
            "off-domain-refusal",
        )
        assertThat(suite.scenarios).allMatch { it.status == HeadlessScenarioStatus.PASS }
        assertThat(turns).hasSize(5)
        assertThat(turns.joinToString("\n")).doesNotContain("sampleIndex")
        assertThat(summary).contains("Status counts: PASS=5 FAIL=0")
        assertThat(summary).contains("Safety: ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)")
    }

    @Test
    fun `given scenario filter, when smoke runs, then only selected scenario is executed`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = WooAiSmokeDeterministicSupportChatService(),
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
            config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                scenarioIds = setOf("orders-read-recent"),
            ),
        ).run()

        assertThat(exit.failureMessage).isNull()
        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val summary = File(outputDirectory, "summary.md").readText()

        assertThat(suite.metadata.scenarioFilter).containsExactly("orders-read-recent")
        assertThat(suite.scenarios.map { it.scenarioId }).containsExactly("orders-read-recent")
        assertThat(summary).contains("Scenario filter: orders-read-recent")
    }

    @Test
    fun `given unknown scenario filter, when smoke runs, then runner rejects the typo`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val error = runCatching {
            WooAiSmokeDeterministicSupportFixtures.runner(
                outputDirectory = outputDirectory,
                chatService = WooAiSmokeDeterministicSupportChatService(),
                toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
                config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                    scenarioIds = setOf("missing-scenario"),
                ),
            ).run()
        }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("missing-scenario")
    }

    @Test
    fun `given failed stream and only negative checks, when smoke runs, then global guard fails scenario`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = ScriptedChatService(List(3) { failedResponse() }),
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
            config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                scenarioResourceName = "negative-only-scenarios.json",
            ),
        ).run()

        assertThat(exit.failureMessage).contains("negative-only")
        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val scenario = suite.scenarios.single()

        assertThat(scenario.status).isEqualTo(HeadlessScenarioStatus.FAIL)
        assertThat(scenario.hardCheckResults.map { it.check.type }).contains(
            HeadlessHardCheckType.NO_FAILED_OUTCOME,
            HeadlessHardCheckType.NO_TURN_ERRORS,
            HeadlessHardCheckType.ASSISTANT_TEXT_NOT_BLANK,
        )
    }

    @Test
    fun `given sampled run where all samples pass, when smoke runs, then sampled classification is pass`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = ScriptedChatService(passOrdersSample() + passOrdersSample()),
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
            config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                sampleCount = 2,
                scenarioIds = setOf("orders-read-recent"),
            ),
        ).run()

        assertThat(exit.failureMessage).isNull()
        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val scenario = suite.scenarios.single()

        assertThat(suite.metadata.sampleCount).isEqualTo(2)
        assertThat(scenario.status).isEqualTo(HeadlessScenarioStatus.PASS)
        assertThat(scenario.sampleSummary?.classification).isEqualTo(HeadlessSampleClassification.PASS)
        assertThat(scenario.sampleSummary?.passCount).isEqualTo(2)
        assertThat(scenario.sampleSummary?.failCount).isEqualTo(0)
    }

    @Test
    fun `given sampled run where all samples fail, when smoke runs, then sampled classification is fail`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = NoToolChatService,
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
            config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                sampleCount = 2,
                scenarioIds = setOf("orders-read-recent"),
            ),
        ).run()

        assertThat(exit.failureMessage).contains("orders-read-recent")
        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val scenario = suite.scenarios.single()

        assertThat(scenario.status).isEqualTo(HeadlessScenarioStatus.FAIL)
        assertThat(scenario.sampleSummary?.classification).isEqualTo(HeadlessSampleClassification.FAIL)
        assertThat(scenario.sampleSummary?.passCount).isEqualTo(0)
        assertThat(scenario.sampleSummary?.failCount).isEqualTo(2)
    }

    @Test
    fun `given sampled run with mixed results, when smoke runs, then sampled classification is flaky`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = ScriptedChatService(
                passOrdersSample() + textOnlySample() + passOrdersSample()
            ),
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
            config = WooAiSmokeDeterministicSupportFixtures.deterministicConfig(
                sampleCount = 3,
                scenarioIds = setOf("orders-read-recent"),
            ),
        ).run()

        assertThat(exit.failureMessage).isNull()
        val suite = json.decodeFromString<HeadlessSuiteRunResult>(
            File(outputDirectory, "run.json").readText()
        )
        val summary = File(outputDirectory, "summary.md").readText()
        val turns = File(outputDirectory, "turns.jsonl").readText()
        val scenario = suite.scenarios.single()

        assertThat(suite.scenarios.map { it.scenarioId }).containsExactly("orders-read-recent")
        assertThat(scenario.status).isEqualTo(HeadlessScenarioStatus.PASS)
        assertThat(scenario.sampleResults.map { it.status }).containsExactly(
            HeadlessScenarioStatus.PASS,
            HeadlessScenarioStatus.FAIL,
            HeadlessScenarioStatus.PASS,
        )
        assertThat(scenario.sampleSummary?.classification).isEqualTo(HeadlessSampleClassification.FLAKY)
        assertThat(scenario.sampleSummary?.passCount).isEqualTo(2)
        assertThat(scenario.sampleSummary?.failCount).isEqualTo(1)
        assertThat(summary).contains("Sample count: 3")
        assertThat(summary).contains("Sampled mode: primary scenario status uses sample 1")
        assertThat(summary).contains("Sampled classification: FLAKY (PASS=2 FAIL=1)")
        assertThat(summary).doesNotContain("Status: FLAKY")
        assertThat(turns).contains(""""sampleIndex":1""")
        assertThat(turns).contains(""""sampleIndex":2""")
        assertThat(turns).contains(""""sampleIndex":3""")
    }

    @Test
    fun `when deterministic hard checks fail, then runner fails directly without baseline comparison`() = runTest {
        val outputDirectory = WooAiSmokeDeterministicSupportFixtures.stableOutputDirectory()

        val exit = WooAiSmokeDeterministicSupportFixtures.runner(
            outputDirectory = outputDirectory,
            chatService = NoToolChatService,
            toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
        ).run()

        assertThat(exit.failureMessage).startsWith("Woo AI deterministic smoke failed: ")
        assertThat(exit.failureMessage).contains("orders-read-recent")
    }

    private fun assertRequiredArtifacts(outputDirectory: File) {
        assertThat(File(outputDirectory, "run.json")).exists()
        assertThat(File(outputDirectory, "turns.jsonl")).exists()
        assertThat(File(outputDirectory, "summary.md")).exists()
    }

    private object NoToolChatService : ChatService {
        override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
            emit(AssistantEvent.TextDelta("I cannot inspect the store from this response."))
            emit(AssistantEvent.Finish(FinishReason.STOP))
        }
    }

    private class ScriptedChatService(
        responses: List<List<AssistantEvent>>,
    ) : ChatService {
        private val responses = responses.toMutableList()

        override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
            check(responses.isNotEmpty()) {
                "No scripted response left for request with ${request.tools.size} tools"
            }
            responses.removeAt(0).forEach { emit(it) }
        }
    }

    private companion object {
        fun failedResponse(): List<AssistantEvent> =
            listOf(AssistantEvent.Failed(ChatStreamError.NETWORK))

        fun passOrdersSample(): List<List<AssistantEvent>> = listOf(
            toolResponse(
                "Checking recent orders.",
                toolCall(0, "orders_list_1", "orders_list", """{"limit":3}"""),
                toolCall(1, "show_cards_orders_1", "show_cards", """{"cards":[]}"""),
            ),
            textResponse("Here are the latest three orders."),
        )

        fun textOnlySample(): List<List<AssistantEvent>> =
            listOf(textResponse("I cannot inspect the store from this response."))

        fun toolResponse(
            text: String,
            vararg calls: AssistantEvent.ToolCallDelta,
        ): List<AssistantEvent> =
            listOf(AssistantEvent.TextDelta(text)) + calls + AssistantEvent.Finish(FinishReason.TOOL_CALLS)

        fun textResponse(text: String): List<AssistantEvent> =
            listOf(AssistantEvent.TextDelta(text), AssistantEvent.Finish(FinishReason.STOP))

        fun toolCall(
            index: Int,
            id: String,
            name: String,
            arguments: String,
        ) = AssistantEvent.ToolCallDelta(
            index = index,
            id = id,
            name = name,
            argumentsDelta = arguments,
        )
    }
}
