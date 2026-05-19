package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter

class ModelRequestHistoryBuilder(
    private val historyBudgeter: HistoryBudgeter,
) {
    fun build(
        systemPrompt: String,
        sessionHistory: AssistantSessionHistory,
        currentUserMessage: String,
    ): ModelRequestHistory {
        val currentUserTurn = AssistantMessage.User(currentUserMessage)
        val rawTranscript = sessionHistory.messages.map { sessionMessage ->
            when (sessionMessage) {
                is AssistantSessionMessage.User -> AssistantMessage.User(sessionMessage.content)
                is AssistantSessionMessage.Assistant -> AssistantMessage.Assistant(sessionMessage.content)
            }
        }
        val budgeted = historyBudgeter.build(
            systemPrompt = AssistantMessage.System(systemPrompt),
            rawTranscript = rawTranscript,
            currentUserTurn = currentUserTurn,
        )
        return ModelRequestHistory(
            messages = budgeted.messages,
            currentUserTurn = currentUserTurn,
        )
    }
}
