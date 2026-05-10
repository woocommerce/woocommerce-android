package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantCardGroupKindTest {
    @Test
    fun `given only order cards, when resolving group kind, then orders metadata is returned`() {
        val metadata = listOf(orderCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_orders)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_orders)
    }

    @Test
    fun `given only product cards, when resolving group kind, then products metadata is returned`() {
        val metadata = listOf(productCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_products)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_products)
    }

    @Test
    fun `given only variation cards, when resolving group kind, then products metadata is returned`() {
        val metadata = listOf(variationCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_products)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_products)
    }

    @Test
    fun `given only stats cards, when resolving group kind, then stats metadata is returned`() {
        val metadata = listOf(statsCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_stats)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_stats)
    }

    @Test
    fun `given only customer cards, when resolving group kind, then customers metadata is returned`() {
        val metadata = listOf(customerCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_customers)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_customers)
    }

    @Test
    fun `given mixed cards, when resolving group kind, then generic metadata is returned`() {
        val metadata = listOf(orderCard(), productCard(), statsCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_generic)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_generic)
    }

    @Test
    fun `given mixed product and variation cards, when resolving group kind, then products metadata is returned`() {
        val metadata = listOf(productCard(), variationCard()).toAssistantCardGroupMetadata()

        assertThat(metadata.titleRes).isEqualTo(R.string.assistant_chat_card_group_products)
        assertThat(metadata.iconRes).isEqualTo(R.drawable.ic_assistant_card_group_products)
    }

    private fun orderCard() = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )

    private fun productCard() = AssistantCard.Product(
        remoteProductId = 456L,
        name = "Woo socks",
        sku = "woo-socks",
        price = "9.99",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "https://example.com/socks.png",
    )

    private fun variationCard() = AssistantCard.Variation(
        parentProductId = 456L,
        variationId = 10L,
        name = "Woo socks - Blue",
        sku = "woo-socks-blue",
        price = "12.99",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "https://example.com/blue-socks.png",
        attributes = listOf(AssistantCard.Variation.Attribute(name = "Color", option = "Blue")),
    )

    private fun statsCard() = AssistantCard.Stats(
        id = "analytics_revenue",
        after = "2026-05-01",
        before = "2026-05-07",
        currency = "USD",
        metrics = listOf(
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalSales,
                value = "170.35",
                chartPoints = emptyList(),
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.NetSales,
                value = "120.15",
                chartPoints = emptyList(),
            ),
        ),
    )

    private fun customerCard() = AssistantCard.Customer(
        remoteCustomerId = 789L,
        name = "Ada Lovelace",
        email = "ada@example.com",
    )
}
