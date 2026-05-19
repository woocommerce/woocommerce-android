package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage

data class ModelRequestHistory(
    val messages: List<AssistantMessage>,
    val currentUserTurn: AssistantMessage.User,
) {
    init {
        require(messages.lastOrNull() == currentUserTurn) {
            "Model request history must end with the current user turn."
        }
    }
}
