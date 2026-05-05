package com.woocommerce.android.aiassistant.ui.cards

internal data class AssistantCardEntry(
    val key: AssistantCardKey,
    val card: AssistantCard,
)

internal data class AssistantCardKey(
    val family: String,
    val id: String,
)
