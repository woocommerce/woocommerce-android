package com.woocommerce.android.aiassistant.core.chat

/**
 * Loop-level normalized error vocabulary surfaced to the agentic loop and any
 * UI layer that consumes the AI assistant. Service-boundary code maps raw
 * exceptions and HTTP status codes into one of these variants so callers never
 * branch on transport types.
 *
 * [OutcomeUnknown] is intentionally separate from [Network] and [Timeout]: it
 * means a write tool was dispatched but the response was lost, so the server
 * may or may not have applied it. That ambiguity drives different retry and
 * UX decisions than a clean transport failure.
 */
sealed class AssistantError {
    data object Network : AssistantError()
    data object Auth : AssistantError()
    data object RateLimit : AssistantError()
    data object BadRequest : AssistantError()
    data object Timeout : AssistantError()
    data object UpstreamFailure : AssistantError()
    data class ToolFailed(val toolName: String, val cause: Throwable? = null) : AssistantError()
    data class InvalidToolCall(val toolName: String) : AssistantError()
    data class OutcomeUnknown(val toolName: String) : AssistantError()
    data object Cancelled : AssistantError()
    data class Unknown(val cause: Throwable? = null) : AssistantError()
}
