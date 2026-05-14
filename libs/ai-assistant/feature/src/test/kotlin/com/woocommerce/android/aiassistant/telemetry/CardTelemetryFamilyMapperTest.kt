package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantActionFamilyValue
import com.automattic.eventhorizon.AiAssistantCardFamilyValue
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardTelemetryFamilyMapperTest {
    @Test
    fun `given assistant cards, when mapped, then card family is bounded`() {
        assertThat(CardTelemetryFamilyMapper.familyOf(orderCard())).isEqualTo(AiAssistantCardFamilyValue.Order)
        assertThat(CardTelemetryFamilyMapper.familyOf(productCard())).isEqualTo(AiAssistantCardFamilyValue.Product)
        assertThat(CardTelemetryFamilyMapper.familyOf(variationCard())).isEqualTo(AiAssistantCardFamilyValue.Variation)
        assertThat(CardTelemetryFamilyMapper.familyOf(customerCard())).isEqualTo(AiAssistantCardFamilyValue.Customer)
        assertThat(CardTelemetryFamilyMapper.familyOf(statsCard())).isEqualTo(AiAssistantCardFamilyValue.AnalyticsStats)
    }

    @Test
    fun `given assistant card actions, when mapped, then action family is bounded`() {
        assertThat(CardTelemetryFamilyMapper.actionOf(AssistantCardAction.OpenOrder(1L)))
            .isEqualTo(AiAssistantActionFamilyValue.OpenOrder)
        assertThat(CardTelemetryFamilyMapper.actionOf(AssistantCardAction.OpenProduct(1L)))
            .isEqualTo(AiAssistantActionFamilyValue.OpenProduct)
        assertThat(CardTelemetryFamilyMapper.actionOf(AssistantCardAction.OpenProductVariation(1L, 2L)))
            .isEqualTo(AiAssistantActionFamilyValue.OpenProductVariation)
        assertThat(CardTelemetryFamilyMapper.actionOf(AssistantCardAction.OpenCustomer(1L)))
            .isEqualTo(AiAssistantActionFamilyValue.OpenCustomer)
        assertThat(CardTelemetryFamilyMapper.actionOf(AssistantCardAction.OpenAnalytics("2026-05-01", "2026-05-12")))
            .isEqualTo(AiAssistantActionFamilyValue.OpenAnalytics)
    }

    private fun orderCard() = AssistantCard.Order(
        remoteOrderId = 1L,
        number = "#1",
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane",
        date = "2026-05-12",
    )

    private fun productCard() = AssistantCard.Product(
        remoteProductId = 1L,
        name = "Socks",
        sku = "sku",
        price = "12.34",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "",
    )

    private fun variationCard() = AssistantCard.Variation(
        parentProductId = 1L,
        variationId = 2L,
        name = "Blue socks",
        sku = "sku",
        price = "12.34",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "",
        attributes = emptyList(),
    )

    private fun customerCard() = AssistantCard.Customer(
        remoteCustomerId = 1L,
        name = "Jane",
        email = "jane@example.com",
    )

    private fun statsCard() = AssistantCard.Stats(
        id = "stats",
        after = "2026-05-01",
        before = "2026-05-12",
        currency = "USD",
        metrics = emptyList(),
    )
}
