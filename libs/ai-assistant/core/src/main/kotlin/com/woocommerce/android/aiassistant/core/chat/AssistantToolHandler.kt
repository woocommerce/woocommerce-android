package com.woocommerce.android.aiassistant.core.chat

fun interface AssistantToolHandler {
    suspend fun execute(call: ToolCall): ToolResult
}
