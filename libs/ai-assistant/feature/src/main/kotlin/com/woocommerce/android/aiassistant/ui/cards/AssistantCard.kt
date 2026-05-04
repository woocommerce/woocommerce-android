package com.woocommerce.android.aiassistant.ui.cards

sealed interface AssistantCard {
    data class Order(
        val remoteOrderId: Long,
        val number: String,
        val status: String,
        val total: String,
        val currency: String,
        val customerName: String,
        val date: String,
    ) : AssistantCard
}
