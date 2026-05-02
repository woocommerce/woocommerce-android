package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardSegmentMapperTest {
    @Test
    fun `given show cards payload, when mapped, then card segments preserve parsed card order`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    orderPayload(id = "1", title = "#1"),
                    orderPayload(id = "2", title = "#2"),
                )
            )
        )

        assertThat(segments).containsExactly(
            AssistantUiSegment.Card(
                AssistantCard.Order(
                    remoteOrderId = 1L,
                    number = "#1",
                    status = "processing",
                    total = "12.34 USD",
                    customerName = "Jane Doe",
                    date = "2026-05-01T10:00:00Z",
                )
            ),
            AssistantUiSegment.Card(
                AssistantCard.Order(
                    remoteOrderId = 2L,
                    number = "#2",
                    status = "processing",
                    total = "12.34 USD",
                    customerName = "Jane Doe",
                    date = "2026-05-01T10:00:00Z",
                )
            ),
        )
    }

    @Test
    fun `given unsupported payload, when mapped, then no segments are returned`() {
        val segments = AssistantCardSegmentMapper.toSegments(
            ShowCardsUiStructured(
                cards = listOf(
                    ShowCardPayload(family = "product", id = "456", title = "Socks"),
                    ShowCardPayload(family = "order", id = "not-a-number", title = "#bad"),
                )
            )
        )

        assertThat(segments).isEmpty()
    }

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        attributes = mapOf(
            "status" to "processing",
            "total" to "12.34",
            "currency" to "USD",
            "date_created" to "2026-05-01T10:00:00Z",
            "customer_name" to "Jane Doe",
        ),
    )
}
