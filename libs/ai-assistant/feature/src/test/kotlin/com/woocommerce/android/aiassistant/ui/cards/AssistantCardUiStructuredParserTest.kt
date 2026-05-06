package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardUiStructuredParserTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
    private val parser = AssistantCardUiStructuredParser(json)

    @Test
    fun `given missing uiStructured, when parsed, then no cards are returned`() {
        assertThat(parser.parse(null)).isEmpty()
    }

    @Test
    fun `given malformed cards field, when parsed, then no cards are returned`() {
        val cards = parser.parse(
            buildJsonObject {
                put("cards", "not an array")
            }
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given malformed card details, when parsed, then no cards are returned`() {
        val cards = parser.parse(
            buildJsonObject {
                putJsonArray("cards") {
                    add(
                        buildJsonObject {
                            put("family", "order")
                            put("id", "123")
                            put("title", "#123")
                            put("details", "not an object")
                        }
                    )
                }
            }
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given valid uiStructured, when parsed, then card entries are returned`() {
        val cards = parser.parse(
            json.encodeToJsonElement(
                ShowCardsUiStructured(
                    cards = listOf(
                        ShowCardPayload(
                            family = "order",
                            id = "123",
                            title = "#123",
                            details = ShowCardDetails.Order(status = "processing"),
                        )
                    )
                )
            )
        )

        assertThat(cards.map { it.key }).containsExactly(AssistantCardKey("order", "123"))
    }
}
