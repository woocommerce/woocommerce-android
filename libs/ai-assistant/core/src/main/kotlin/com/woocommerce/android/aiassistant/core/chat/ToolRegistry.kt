package com.woocommerce.android.aiassistant.core.chat

interface ToolRegistry {
    fun descriptors(): List<ToolDescriptor>
    suspend fun execute(call: ToolCall): ToolResult
}
