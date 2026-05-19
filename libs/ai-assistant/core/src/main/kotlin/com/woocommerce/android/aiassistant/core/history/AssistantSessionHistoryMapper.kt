package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage

class AssistantSessionHistoryMapper {
    fun appendTurn(
        baseHistory: AssistantSessionHistory,
        modelTurnMessages: List<AssistantMessage>,
    ): AssistantSessionHistory {
        val sessionMessages = modelTurnMessages.mapNotNull { message ->
            when (message) {
                is AssistantMessage.User -> AssistantSessionMessage.User(message.content)
                is AssistantMessage.Assistant ->
                    message
                        .content
                        ?.takeIf { content -> content.isNotBlank() }
                        ?.let { content -> AssistantSessionMessage.Assistant(content) }
                is AssistantMessage.System,
                is AssistantMessage.Tool -> null
            }
        }
        return baseHistory.append(sessionMessages)
    }

    fun appendCancelledTurn(
        baseHistory: AssistantSessionHistory,
        userMessage: String,
        assistantText: String?,
    ): AssistantSessionHistory {
        val sessionMessages = buildList {
            add(AssistantSessionMessage.User(userMessage))
            assistantText
                ?.takeIf { text -> text.isNotBlank() }
                ?.let { text -> add(AssistantSessionMessage.Assistant(text)) }
        }
        return baseHistory.append(sessionMessages)
    }
}
