package com.woocommerce.android.aiassistant.core.chat

/** Single chat turn submitted to [ChatService]. */
data class ChatRequest(
    val messages: List<AssistantMessage>,
    val tools: List<ToolDefinition> = emptyList(),
)
