package com.woocommerce.android.aiassistant.ui.cards

data class AssistantCardEntry(
    val key: AssistantCardKey,
    val card: AssistantCard,
)

data class AssistantCardKey(
    val family: String,
    val id: String,
)
