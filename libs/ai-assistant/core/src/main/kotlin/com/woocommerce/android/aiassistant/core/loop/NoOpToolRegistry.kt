package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult

class NoOpToolRegistry : ToolRegistry {
    override fun descriptors(): List<ToolDescriptor> = emptyList()
    override suspend fun execute(call: ToolCall): ToolResult =
        ToolResult.ValidationError(call.id, "No tools registered")
}
