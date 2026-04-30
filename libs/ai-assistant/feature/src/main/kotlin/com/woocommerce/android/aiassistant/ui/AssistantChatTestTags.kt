package com.woocommerce.android.aiassistant.ui

object AssistantChatTestTags {
    const val THREAD = "assistant_chat_thread"
    const val INPUT = "assistant_chat_input"

    fun message(id: String): String = "assistant_chat_message_$id"
}
