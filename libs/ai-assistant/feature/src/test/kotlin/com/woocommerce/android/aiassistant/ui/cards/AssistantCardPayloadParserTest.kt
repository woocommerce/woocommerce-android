package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
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
                        details = ShowCardDetails.Order(
                            status = "processing",
                            total = "12.34",
                            currency = "USD",
                            dateCreated = "2026-05-01T10:00:00Z",
                            customerName = "Jane Doe",
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
                total = "12.34",
                currency = "USD",
                customerName = "Jane Doe",
                date = "2026-05-01T10:00:00Z",
            )
        )
    }

    @Test
    fun `given order payload without status detail, when parsed, then status is empty`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "order",
                        id = "123",
                        title = "#1001",
                        details = ShowCardDetails.Order(total = "12.34"),
                    )
                )
            )
        )

        assertThat(cards.single()).isEqualTo(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#1001",
                status = "",
                total = "12.34",
                currency = "",
                customerName = "",
                date = "",
            )
        )
    }

    @Test
    fun `given order payload, when parsed as entries, then raw family id key is preserved`() {
        val entries = AssistantCardPayloadParser.parseEntries(
            ShowCardsUiStructured(
                cards = listOf(orderPayload(id = "00123", title = "#123"))
            )
        )

        assertThat(entries.single().key).isEqualTo(AssistantCardKey(family = "order", id = "00123"))
        assertThat(entries.single().card).isEqualTo(
            AssistantCard.Order(
                remoteOrderId = 123L,
                number = "#123",
                status = "processing",
                total = "",
                currency = "",
                customerName = "",
                date = "",
            )
        )
    }

    @Test
    fun `given product payload, when parsed, then product card contains displayed fields`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "product",
                        id = "456",
                        title = "Socks",
                        details = ShowCardDetails.Product(
                            sku = "woo-socks",
                            price = "9.99",
                            stockStatus = "instock",
                            status = "publish",
                            imageUrl = "https://example.com/socks.png",
                        ),
                    )
                )
            )
        )

        assertThat(cards).containsExactly(
            AssistantCard.Product(
                remoteProductId = 456L,
                name = "Socks",
                sku = "woo-socks",
                price = "9.99",
                stockStatus = "instock",
                status = "publish",
                imageUrl = "https://example.com/socks.png",
            )
        )
    }

    @Test
    fun `given unsupported and invalid payloads, when parsed, then they are ignored`() {
        val cards = AssistantCardPayloadParser.parse(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "customer",
                        id = "456",
                        title = "Customer",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "product",
                        id = "not-a-number",
                        title = "Bad product",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "order",
                        id = "0",
                        title = "#0",
                        details = ShowCardDetails.Order(),
                    ),
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
        details = ShowCardDetails.Order(status = "processing"),
    )
}
