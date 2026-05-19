package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.FinishReason
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
        assertThat(summary).contains("Status counts: PASS=5 FAIL=0")
        assertThat(summary).contains("Safety: ScriptedHeadlessSafetyOrchestrator(default=CANCELLED)")
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
}
