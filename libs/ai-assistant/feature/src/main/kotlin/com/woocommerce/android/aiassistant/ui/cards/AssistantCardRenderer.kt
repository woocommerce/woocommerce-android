package com.woocommerce.android.aiassistant.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AssistantCardRenderer {
    @Composable
    fun Card(
        card: AssistantCard,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    )
}
