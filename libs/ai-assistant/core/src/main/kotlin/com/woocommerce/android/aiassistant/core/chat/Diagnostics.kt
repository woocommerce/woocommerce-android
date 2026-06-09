package com.woocommerce.android.aiassistant.core.chat

data class Diagnostics(
    val transport: TransportDiagnostics? = null,
    val tool: ToolDiagnostics? = null,
)

data class TransportDiagnostics(
    val httpStatus: Int? = null,
    val requestId: String? = null,
    val retryAfterMs: Long? = null,
    val bodySnippet: String? = null,
)

data class ToolDiagnostics(
    val toolName: String? = null,
    val failureKind: ToolFailureKind? = null,
    val retryable: Boolean? = null,
    val source: ToolFailureSource? = null,
)

enum class ToolFailureSource {
    TOOL_RESULT,
    HANDLER_EXCEPTION,
    INVALID_TOOL_CALL,
}
