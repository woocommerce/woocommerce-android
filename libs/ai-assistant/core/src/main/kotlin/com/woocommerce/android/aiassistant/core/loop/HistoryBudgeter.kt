package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage

fun interface HistoryBudgeter {
    fun build(
        systemPrompt: AssistantMessage.System,
        rawTranscript: List<AssistantMessage>,
        currentUserTurn: AssistantMessage.User,
    ): BudgetedHistory
}
