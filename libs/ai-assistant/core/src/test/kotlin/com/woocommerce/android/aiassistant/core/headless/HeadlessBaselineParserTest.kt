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
              "scenarios": [
                {
                  "id": "orders-processing",
                  "category": "read",
                  "scope": "ORDERS",
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

        val scenario = baseline.scenarios.single()
        assertThat(scenario.id).isEqualTo("orders-processing")
        assertThat(scenario.category).isEqualTo("read")
        assertThat(scenario.scope).isEqualTo(ToolScope.ORDERS)
        assertThat(scenario.turns.single().hardChecks.single())
            .isEqualTo(HeadlessHardCheck(HeadlessHardCheckType.TOOL_CALLED, "orders_list"))
    }

    @Test
    fun `given smoke baseline missing category, when parse is used, then parsing fails`() {
        assertThatThrownBy {
            parser.parse(
                """
                {
                  "scenarios": [
                    {"id": "bad", "scope": "GLOBAL", "turns": [], "hardChecks": []}
                  ]
                }
                """.trimIndent()
            )
        }.hasMessageContaining("category")
    }

    @Test
    fun `given complete smoke baseline, when parse is used, then required fields are decoded`() {
        val baseline = parser.parse(
            """
            {
              "scenarios": [
                {
                  "id": "orders-read",
                  "category": "read",
                  "scope": "ORDERS",
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
        assertThat(scenario.category).isEqualTo("read")
        assertThat(scenario.scope).isEqualTo(ToolScope.ORDERS)
        assertThat(scenario.turns.single().hardChecks.single().type).isEqualTo(HeadlessHardCheckType.TOOL_CALLED)
    }

    @Test
    fun `given approved baseline json, when parsing, then metadata and scenario checks are decoded`() {
        val baseline = parser.parseApprovedBaseline(
            """
            {
              "metadata": {
                "modelId": "gpt-4o",
                "promptVersion": "1.0.0",
                "toolCatalogVersion": "1.0.0"
              },
              "scenarios": [
                {
                  "scenarioId": "write-confirmation-declined",
                  "category": "write",
                  "approvedHardChecks": [
                    { "type": "OUTCOME_EQUALS", "value": "STOPPED" },
                    { "type": "CONFIRMATION_DECISION_EQUALS", "value": "CANCELLED" },
                    { "type": "TOOL_RESULT_KIND_EQUALS", "value": "orders_update:REJECTED_BY_SAFETY" }
                  ],
                  "sampleExpectation": {
                    "sampleCount": 3,
                    "acceptedClassification": "FLAKY"
                  },
                  "knownFailure": {
                    "reason": "Model currently asks for confirmation after safety cancellation.",
                    "expectedFailedHardChecks": [
                      { "type": "OUTCOME_EQUALS", "value": "STOPPED" }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertThat(baseline.metadata.modelId).isEqualTo("gpt-4o")
        assertThat(baseline.scenarios.single().category).isEqualTo("write")
        assertThat(baseline.scenarios.single().knownFailure?.expectedFailedHardChecks?.single()?.value)
            .isEqualTo("STOPPED")
        assertThat(baseline.scenarios.single().approvedHardChecks.map { it.value })
            .contains("STOPPED", "CANCELLED", "orders_update:REJECTED_BY_SAFETY")
        assertThat(baseline.scenarios.single().sampleExpectation?.sampleCount).isEqualTo(3)
        assertThat(baseline.scenarios.single().sampleExpectation?.acceptedClassification)
            .isEqualTo(HeadlessSampleClassification.FLAKY)
    }
}
