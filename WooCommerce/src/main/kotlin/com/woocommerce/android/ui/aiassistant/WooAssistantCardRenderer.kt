package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils

class WooAssistantCardRenderer internal constructor(
    currencyFormatter: AiAssistantCurrencyFormatter,
    dateUtils: DateUtils,
) : AssistantCardRenderer {
    private val orderCardRenderer = AiAssistantOrderCardRenderer(currencyFormatter, dateUtils)
    private val productCardRenderer = AiAssistantProductCardRenderer(currencyFormatter)
    private val variationCardRenderer = AiAssistantVariationCardRenderer(currencyFormatter)
    private val statsCardRenderer = AiAssistantStatsCardRenderer(currencyFormatter)
    private val customerCardRenderer = AiAssistantCustomerCardRenderer()

    constructor(
        currencyFormatter: CurrencyFormatter,
        dateUtils: DateUtils,
    ) : this(WooAiAssistantCurrencyFormatter(currencyFormatter), dateUtils)

    @Composable
    override fun Card(
        card: AssistantCard,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        when (card) {
            is AssistantCard.Order -> orderCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Product -> productCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Variation -> variationCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Stats -> statsCardRenderer.Card(card, onAction, modifier)
            is AssistantCard.Customer -> customerCardRenderer.Card(card, onAction, modifier)
        }
    }
}
