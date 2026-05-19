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
        val rawTranscript = sessionHistory.messages.flatMap { sessionMessage ->
            when (sessionMessage) {
                is AssistantSessionMessage.User -> listOf(AssistantMessage.User(sessionMessage.content))
                is AssistantSessionMessage.Assistant -> listOf(AssistantMessage.Assistant(sessionMessage.content))
                is AssistantSessionMessage.ToolExchange -> listOf(
                    AssistantMessage.Assistant(
                        content = sessionMessage.assistantContent,
                        toolCalls = sessionMessage.toolCalls,
                    )
                ) + sessionMessage.toolResults
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
