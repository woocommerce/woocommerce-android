package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.headless.HeadlessBaselineParser
import com.woocommerce.android.aiassistant.core.headless.HeadlessHardCheckType
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenarioCategory
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeScenarioMapperTest {
    @Test
    fun `given debug resources, when loading scenarios, then all smoke categories are present`() {
        val scenarios = mapper().loadScenarioSpecs()

        assertThat(scenarios.map { it.category }.toSet()).containsAll(
            setOf(
                HeadlessScenarioCategory.ORDERS_READ,
                HeadlessScenarioCategory.PRODUCTS_READ,
                HeadlessScenarioCategory.ANALYTICS_READ,
                HeadlessScenarioCategory.WRITE_CONFIRMATION,
                HeadlessScenarioCategory.OFF_DOMAIN_REFUSAL,
            )
        )
    }

    @Test
    fun `given debug resources, when loading scenarios, then all hard check types have evaluator coverage`() {
        val usedTypes = mapper().loadScenarioSpecs()
            .flatMap { scenario -> scenario.turns.flatMap { it.hardChecks } + scenario.hardChecks }
            .map { it.type }
            .toSet()

        assertThat(usedTypes).isSubsetOf(
            HeadlessHardCheckType.OUTCOME_EQUALS,
            HeadlessHardCheckType.ASSISTANT_TEXT_CONTAINS,
            HeadlessHardCheckType.ASSISTANT_TEXT_NOT_CONTAINS,
            HeadlessHardCheckType.ASSISTANT_REFUSAL,
            HeadlessHardCheckType.TOOL_CALLED,
            HeadlessHardCheckType.TOOL_NOT_CALLED,
            HeadlessHardCheckType.TOOL_CALL_COUNT_AT_MOST,
            HeadlessHardCheckType.TOOL_RESULT_KIND_EQUALS,
            HeadlessHardCheckType.CONFIRMATION_DECISION_EQUALS,
            HeadlessHardCheckType.TOOL_ARGUMENT_JSON_CONTAINS,
        )
    }

    @Test
    fun `given scenario spec, when mapping, then selected site id is carried into session context`() {
        val scenario = mapper(selectedSiteId = SELECTED_SITE_ID).toHeadlessScenario(
            mapper().loadScenarioSpecs().first { it.scope == ToolScope.ORDERS }
        )

        assertThat(scenario.context.siteId).isEqualTo(SELECTED_SITE_ID)
        assertThat(scenario.initialHistory.single()).isEqualTo(AssistantMessage.System("System prompt"))
    }

    @Test
    fun `given scenario resource, when parsed strictly from classpath, then it is readable`() {
        val source = requireNotNull(javaClass.classLoader?.getResource("woo-ai-smoke/live-scenarios.json")).readText()
        val baseline = HeadlessBaselineParser(json).parseStrict(source)

        assertThat(baseline.scenarios).hasSize(5)
    }

    @Test
    fun `given approved baseline resource, when parsed from classpath, then it is readable`() {
        val source = requireNotNull(javaClass.classLoader?.getResource("woo-ai-smoke/support-baseline.json")).readText()
        val baseline = HeadlessBaselineParser(json).parseApprovedBaseline(source)

        assertThat(baseline.scenarios.map { it.scenarioId }).containsExactlyInAnyOrder(
            "orders-read-recent",
            "products-search-card",
            "analytics-orders-this-month",
            "write-confirmation-declined",
            "off-domain-refusal",
        )
    }

    private fun mapper(selectedSiteId: Long = 1L) = WooAiSmokeScenarioMapper(
        toolRegistry = StaticToolRegistry,
        toolCatalogSelector = DefaultToolCatalogSelector(),
        systemPromptProvider = StaticSystemPromptProvider,
        json = json,
        selectedSiteId = selectedSiteId,
        resourceName = "live-scenarios.json",
    )

    private object StaticSystemPromptProvider : AssistantSystemPromptProvider {
        override fun systemPrompt(todayIsoDate: String?): String = "System prompt"
    }

    private object StaticToolRegistry : ToolRegistry {
        private val descriptors = listOf(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "product_variations_update",
            "analytics_orders",
            "show_cards",
        ).map { toolName ->
            ToolDescriptor(
                name = toolName,
                description = "Test tool",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            )
        }

        override fun descriptors(): List<ToolDescriptor> = descriptors

        override suspend fun execute(call: ToolCall): ToolResult =
            error("Not used by scenario mapper tests")
    }

    private companion object {
        const val SELECTED_SITE_ID = 123L

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
