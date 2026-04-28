package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import javax.inject.Inject

class WooCommerceToolRegistry @Inject constructor(
    handlers: Set<@JvmSuppressWildcards AssistantToolHandler>,
) : ToolRegistry {

    private val handlersByName: Map<String, AssistantToolHandler> = handlers.associateBy { it.descriptor.name }

    override fun descriptors(): List<ToolDescriptor> = handlersByName.values.map { it.descriptor }

    override suspend fun execute(call: ToolCall): ToolResult {
        val handler = handlersByName[call.name]
            ?: return ToolResult.ValidationError(call.id, "No handler registered for tool: ${call.name}")
        return handler.execute(call)
    }
}
