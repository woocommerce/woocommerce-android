package com.woocommerce.android.aiassistant.core.chat

interface AssistantToolHandler {
    val descriptor: ToolDescriptor
    suspend fun execute(call: ToolCall): ToolResult
}
