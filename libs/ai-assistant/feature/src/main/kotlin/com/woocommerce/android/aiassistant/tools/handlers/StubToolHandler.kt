package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult

abstract class StubToolHandler : AssistantToolHandler {
    override suspend fun execute(call: ToolCall): ToolResult =
        ToolResult.ValidationError(
            toolCallId = call.id,
            reason = "Tool ${descriptor.name} is not yet implemented",
        )
}
