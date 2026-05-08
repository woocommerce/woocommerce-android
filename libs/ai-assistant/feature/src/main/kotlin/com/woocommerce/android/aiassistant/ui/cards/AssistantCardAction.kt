package com.woocommerce.android.aiassistant.ui.cards

sealed interface AssistantCardAction {
    data class OpenOrder(val remoteOrderId: Long) : AssistantCardAction
    data class OpenProduct(val remoteProductId: Long) : AssistantCardAction
    data class OpenAnalytics(
        val after: String,
        val before: String,
    ) : AssistantCardAction
}
