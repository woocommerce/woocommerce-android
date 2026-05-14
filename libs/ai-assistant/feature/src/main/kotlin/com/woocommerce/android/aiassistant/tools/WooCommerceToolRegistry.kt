package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolDiagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolFailureSource
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class WooCommerceToolRegistry @Inject constructor(
    handlers: Set<@JvmSuppressWildcards AssistantToolHandler>,
) : ToolRegistry {

    private val handlersByName: Map<String, AssistantToolHandler> = handlers.associateBy { it.descriptor.name }

    init {
        val names = handlers.map { it.descriptor.name }
        require(names.size == names.toSet().size) {
            "Duplicate tool names: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}"
        }
    }

    override fun descriptors(): List<ToolDescriptor> = handlersByName.values.map { it.descriptor }

    override suspend fun execute(call: ToolCall): ToolResult {
        val handler = handlersByName[call.name]
            ?: return ToolResult.ValidationError(call.id, "No handler registered for tool: ${call.name}")
        return try {
            handler.execute(call)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            ToolResult.TransportError(
                toolCallId = call.id,
                retryable = false,
                kind = ToolFailureKind.OUTCOME_UNKNOWN,
                diagnostics = Diagnostics(
                    tool = ToolDiagnostics(
                        toolName = call.name,
                        failureKind = ToolFailureKind.OUTCOME_UNKNOWN,
                        retryable = false,
                        source = ToolFailureSource.HANDLER_EXCEPTION,
                    )
                )
            )
        }
    }
}
