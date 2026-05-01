package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardPayloadParserTest {
    @Test
    fun `given order payload, when parsed, then order card contains displayed fields`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "order",
                        id = "123",
                        title = "#1001",
                        subtitle = "processing",
                        badges = listOf("processing"),
                        attributes = mapOf(
                            "status" to "processing",
                            "total" to "12.34",
                            "currency" to "USD",
                            "date_created" to "2026-05-01T10:00:00Z",
                            "customer_name" to "Jane Doe",
                        ),
                    )
                )
            )
        )

        assertThat(cards).containsExactly(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#1001",
                status = "processing",
                total = "12.34 USD",
                customerName = "Jane Doe",
                date = "2026-05-01T10:00:00Z",
            )
        )
    }

    @Test
    fun `given order payload without status attribute, when parsed, then subtitle is used`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "order",
                        id = "123",
                        title = "#1001",
                        subtitle = "completed",
                        attributes = mapOf("total" to "12.34"),
                    )
                )
            )
        )

        assertThat(cards.single()).isEqualTo(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#1001",
                status = "completed",
                total = "12.34",
                customerName = "",
                date = "",
            )
        )
    }

    @Test
    fun `given product and invalid order payloads, when parsed, then they are ignored`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(family = "product", id = "456", title = "Socks"),
                    ShowCardPayload(family = "order", id = "not-a-number", title = "#bad"),
                    ShowCardPayload(family = "order", id = "0", title = "#0"),
                )
            )
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun `given multiple order payloads, when parsed, then input order is preserved`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    orderPayload(id = "1", title = "#1"),
                    orderPayload(id = "2", title = "#2"),
                )
            )
        )

        assertThat(cards.map { (it as AssistantCard.Order).remoteOrderId }).containsExactly(1L, 2L)
    }

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        attributes = mapOf("status" to "processing"),
    )
}
