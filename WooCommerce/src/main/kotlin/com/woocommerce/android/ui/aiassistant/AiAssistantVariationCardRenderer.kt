package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.products.compose.ProductSummaryRow

internal class AiAssistantVariationCardRenderer {
    @Composable
    fun Card(
        card: AssistantCard.Variation,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        ProductSummaryRow(
            title = card.name,
            imageUrl = card.imageUrl,
            onClick = {},
            modifier = modifier,
            supportingContent = {},
        )
    }
}
