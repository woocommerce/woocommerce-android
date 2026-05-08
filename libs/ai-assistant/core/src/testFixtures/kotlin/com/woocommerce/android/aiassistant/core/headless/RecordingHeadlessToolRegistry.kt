package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult

class RecordingHeadlessToolRegistry(
    private val descriptors: List<ToolDescriptor>,
    private val results: Map<String, ToolResult>,
) : ToolRegistry {
    val calls = mutableListOf<ToolCall>()

    override fun descriptors(): List<ToolDescriptor> = descriptors

    override suspend fun execute(call: ToolCall): ToolResult {
        calls += call
        return results[call.name]
            ?: ToolResult.ValidationError(call.id, "No scripted result for tool: ${call.name}")
    }
}
