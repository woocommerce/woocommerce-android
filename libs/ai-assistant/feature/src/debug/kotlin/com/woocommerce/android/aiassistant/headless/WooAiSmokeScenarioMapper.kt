package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineParser
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenario
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioSpec
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import kotlinx.serialization.json.Json

internal class WooAiSmokeScenarioMapper(
    private val toolRegistry: ToolRegistry,
    private val toolCatalogSelector: ToolCatalogSelector,
    private val systemPromptProvider: AssistantSystemPromptProvider,
    private val json: Json,
    private val selectedSiteId: Long,
) {
    fun loadScenarioSpecs(): List<HeadlessScenarioSpec> {
        val source = requireNotNull(
            javaClass.classLoader?.getResource("woo-ai-smoke/scenarios.json")
        ) { "Missing woo-ai-smoke/scenarios.json" }.readText()
        return HeadlessBaselineParser(json).parseStrict(source).scenarios
    }

    fun toHeadlessScenario(spec: HeadlessScenarioSpec): HeadlessScenario =
        HeadlessScenario(
            id = spec.id,
            turns = spec.turns,
            initialHistory = listOf(AssistantMessage.System(systemPromptProvider.systemPrompt())),
            context = SessionContext(
                siteId = selectedSiteId,
                catalogSnapshot = toolCatalogSelector.select(spec.scope, toolRegistry.descriptors()),
            ),
        )
}
