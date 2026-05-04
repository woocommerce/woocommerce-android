package com.woocommerce.android.aiassistant.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AssistantCardRenderer {
    @Composable
    fun OrderCard(
        card: AssistantCard.Order,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    )
}
