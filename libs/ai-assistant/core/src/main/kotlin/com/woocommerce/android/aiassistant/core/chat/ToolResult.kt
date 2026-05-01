package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonElement

sealed interface ToolResult {
    val toolCallId: String

    data class Success(
        override val toolCallId: String,
        val structured: JsonElement,
        val uiStructured: JsonElement? = null,
    ) : ToolResult

    data class ValidationError(
        override val toolCallId: String,
        val reason: String,
    ) : ToolResult

    data class RejectedBySafety(
        override val toolCallId: String,
    ) : ToolResult

    data class TransportError(
        override val toolCallId: String,
        val retryable: Boolean,
    ) : ToolResult
}
