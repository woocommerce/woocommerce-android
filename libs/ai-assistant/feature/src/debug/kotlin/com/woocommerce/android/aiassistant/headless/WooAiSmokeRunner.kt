package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.headless.ScriptedHeadlessSafetyOrchestrator
import com.woocommerce.android.aiassistant.core.headless.WooAssistantHeadless
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import kotlinx.serialization.json.Json
import java.io.File
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
        outputDirectory.mkdirs()
        createHarness()
        return WooAiSmokeRunExit(
            artifactsDirectory = outputDirectory,
            failureMessage = null,
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
}
