package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.CardRef

data class BudgetedHistory(
    val messages: List<AssistantMessage>,
    val retainedEntityRefs: List<CardRef> = emptyList(),
)
