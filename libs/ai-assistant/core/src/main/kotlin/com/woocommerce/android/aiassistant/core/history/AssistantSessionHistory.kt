package com.woocommerce.android.aiassistant.core.history

data class AssistantSessionHistory(
    val messages: List<AssistantSessionMessage> = emptyList(),
) {
    fun append(newMessages: List<AssistantSessionMessage>) = copy(messages = messages + newMessages)

    companion object {
        val Empty = AssistantSessionHistory()
    }
}

sealed interface AssistantSessionMessage {
    data class User(val content: String) : AssistantSessionMessage
    data class Assistant(val content: String) : AssistantSessionMessage
}
