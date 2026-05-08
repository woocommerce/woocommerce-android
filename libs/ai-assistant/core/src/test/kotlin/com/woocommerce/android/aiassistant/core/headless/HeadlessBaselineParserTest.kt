package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HeadlessBaselineParserTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun `given baseline json, when parsing, then scenario contract is decoded`() {
        val baseline = HeadlessBaselineParser(json).parse(
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
}
