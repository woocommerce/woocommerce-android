package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardSegmentMapperTest {
    @Test
    fun `given order and product payloads, when mapped, then card segments preserve parsed card order`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    orderPayload(id = "1", title = "#1"),
                    productPayload(id = "2", title = "Socks"),
                )
            )
        )

        assertThat(segments).containsExactly(
            AssistantUiSegment.Card(
                AssistantCard.Order(
                    remoteOrderId = 1L,
                    number = "#1",
                    status = "processing",
                    total = "12.34",
                    currency = "USD",
                    customerName = "Jane Doe",
                    date = "2026-05-01T10:00:00Z",
                )
            ),
            AssistantUiSegment.Card(
                AssistantCard.Product(
                    remoteProductId = 2L,
                    name = "Socks",
                    sku = "woo-socks",
                    price = "9.99",
                    stockStatus = "instock",
                    status = "publish",
                    imageUrl = "https://example.com/socks.png",
                )
            ),
        )
    }

    @Test
    fun `given unsupported payload, when mapped, then no segments are returned`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(
                        family = "customer",
                        id = "456",
                        title = "Customer",
                        details = ShowCardDetails.Product(),
                    ),
                    ShowCardPayload(
                        family = "order",
                        id = "not-a-number",
                        title = "#bad",
                        details = ShowCardDetails.Order(),
                    ),
                )
            )
        )

        assertThat(segments).isEmpty()
    }

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        details = ShowCardDetails.Order(
            status = "processing",
            total = "12.34",
            currency = "USD",
            dateCreated = "2026-05-01T10:00:00Z",
            customerName = "Jane Doe",
        ),
    )

    private fun productPayload(id: String, title: String) = ShowCardPayload(
        family = "product",
        id = id,
        title = title,
        details = ShowCardDetails.Product(
            sku = "woo-socks",
            price = "9.99",
            stockStatus = "instock",
            status = "publish",
            imageUrl = "https://example.com/socks.png",
        ),
    )
}
