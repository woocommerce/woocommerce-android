package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import com.woocommerce.android.util.CurrencyFormatter

class WooAssistantCardRenderer internal constructor(
    currencyFormatter: AiAssistantCurrencyFormatter,
) : AssistantCardRenderer {
    private val orderCardRenderer = AiAssistantOrderCardRenderer(currencyFormatter)
    private val productCardRenderer = AiAssistantProductCardRenderer(currencyFormatter)
    private val statsCardRenderer = AiAssistantStatsCardRenderer(currencyFormatter)
    private val customerCardRenderer = AiAssistantCustomerCardRenderer()

    constructor(currencyFormatter: CurrencyFormatter) : this(WooAiAssistantCurrencyFormatter(currencyFormatter))

    @Composable
    override fun Card(
        card: AssistantCard,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        when (card) {
            is AssistantCard.Order -> orderCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Product -> productCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Stats -> statsCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Customer -> customerCardRenderer.Card(card, onAction, modifier)
        }
    }
}
