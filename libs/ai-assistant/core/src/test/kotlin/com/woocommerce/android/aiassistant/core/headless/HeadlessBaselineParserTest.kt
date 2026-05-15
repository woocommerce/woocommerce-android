package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.loop.ToolScope
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class HeadlessBaselineParserTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val parser = HeadlessBaselineParser(json)

    @Test
    fun `given baseline json, when parsing, then scenario contract is decoded`() {
        val baseline = parser.parse(
            """
            {
              "version": 1,
              "scenarios": [
                {
                  "id": "orders-processing",
                  "turns": [
                    {
                      "userMessage": "How many processing orders do I have?",
                      "hardChecks": [
                        { "type": "TOOL_CALLED", "value": "orders_list" }
                      ]
                    }
                  ],
                  "hardChecks": [
                    { "type": "OUTCOME_EQUALS", "value": "COMPLETED" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertThat(baseline.version).isEqualTo(1)
        assertThat(baseline.scenarios.single().id).isEqualTo("orders-processing")
        assertThat(baseline.scenarios.single().turns.single().hardChecks.single())
            .isEqualTo(HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALLED, "orders_list"))
    }

    @Test
    fun `given legacy baseline, when parse is used, then legacy fields are explicit`() {
        val baseline = parser.parse(
            """{"version":1,"scenarios":[{"id":"legacy","turns":[{"userMessage":"Hi"}]}]}"""
        )

        val scenario = baseline.scenarios.single()
        assertThat(scenario.category).isEqualTo(HeadlessScenarioCategory.LEGACY_SCRIPTED)
        assertThat(scenario.scope).isEqualTo(ToolScope.GLOBAL)
        assertThat(scenario.hardChecks).isEmpty()
        assertThat(scenario.smokeFixture).isNull()
        assertThat(scenario.turns.single().hardChecks).isEmpty()
    }

    @Test
    fun `given smoke baseline missing category, when strict parse is used, then parsing fails`() {
        assertThatThrownBy {
            parser.parseStrict(
                """
                {
                  "version": 1,
                  "scenarios": [
                    {"id": "bad", "scope": "GLOBAL", "turns": [], "hardChecks": [], "smokeFixture": null}
                  ]
                }
                """.trimIndent()
            )
        }.hasMessageContaining("category")
    }

    @Test
    fun `given complete smoke baseline, when strict parse is used, then required fields are decoded`() {
        val baseline = parser.parseStrict(
            """
            {
              "version": 1,
              "scenarios": [
                {
                  "id": "orders-read",
                  "category": "ORDERS_READ",
                  "scope": "ORDERS",
                  "smokeFixture": null,
                  "turns": [
                    {
                      "userMessage": "Show recent orders",
                      "hardChecks": [
                        { "type": "TOOL_CALLED", "value": "orders_list" }
                      ]
                    }
                  ],
                  "hardChecks": [
                    { "type": "OUTCOME_EQUALS", "value": "COMPLETED" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val scenario = baseline.scenarios.single()
        assertThat(scenario.category).isEqualTo(HeadlessScenarioCategory.ORDERS_READ)
        assertThat(scenario.scope).isEqualTo(ToolScope.ORDERS)
        assertThat(scenario.turns.single().hardChecks.single().type).isEqualTo(HeadlessHardCheckType.TOOL_CALLED)
    }

    @Test
    fun `given approved baseline json, when parsing, then metadata and scenario checks are decoded`() {
        val baseline = parser.parseApprovedBaseline(
            """
            {
              "version": 1,
              "metadata": {
                "modelId": "gpt-4o",
                "promptVersion": "1.0.0",
                "toolCatalogVersion": "1.0.0"
              },
              "scenarios": [
                {
                  "scenarioId": "write-confirmation-declined",
                  "category": "WRITE_CONFIRMATION",
                  "approvedStatus": "PASS",
                  "approvedHardChecks": [
                    { "type": "OUTCOME_EQUALS", "value": "STOPPED" },
                    { "type": "CONFIRMATION_DECISION_EQUALS", "value": "CANCELLED" },
                    { "type": "TOOL_RESULT_KIND_EQUALS", "value": "orders_update:REJECTED_BY_SAFETY" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertThat(baseline.metadata.modelId).isEqualTo("gpt-4o")
        assertThat(baseline.scenarios.single().category).isEqualTo(HeadlessScenarioCategory.WRITE_CONFIRMATION)
        assertThat(baseline.scenarios.single().approvedHardChecks.map { it.value })
            .contains("STOPPED", "CANCELLED", "orders_update:REJECTED_BY_SAFETY")
    }
}
