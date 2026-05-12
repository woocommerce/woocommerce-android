package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantActionFamilyValue
import com.automattic.eventhorizon.AiAssistantCardFamilyValue
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction

object CardTelemetryFamilyMapper {
    fun familyOf(card: AssistantCard): AiAssistantCardFamilyValue = when (card) {
        is AssistantCard.Order -> AiAssistantCardFamilyValue.Order
        is AssistantCard.Product -> AiAssistantCardFamilyValue.Product
        is AssistantCard.Variation -> AiAssistantCardFamilyValue.Variation
        is AssistantCard.Customer -> AiAssistantCardFamilyValue.Customer
        is AssistantCard.Stats -> AiAssistantCardFamilyValue.AnalyticsStats
    }

    fun actionOf(action: AssistantCardAction): AiAssistantActionFamilyValue = when (action) {
        is AssistantCardAction.OpenOrder -> AiAssistantActionFamilyValue.OpenOrder
        is AssistantCardAction.OpenProduct -> AiAssistantActionFamilyValue.OpenProduct
        is AssistantCardAction.OpenProductVariation -> AiAssistantActionFamilyValue.OpenProductVariation
        is AssistantCardAction.OpenCustomer -> AiAssistantActionFamilyValue.OpenCustomer
        is AssistantCardAction.OpenAnalytics -> AiAssistantActionFamilyValue.OpenAnalytics
    }
}
