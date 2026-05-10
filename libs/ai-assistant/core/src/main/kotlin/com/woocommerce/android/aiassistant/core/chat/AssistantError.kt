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
    data class Network(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class Auth(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class RateLimit(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class BadRequest(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class Timeout(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class UpstreamFailure(val diagnostics: Diagnostics = Diagnostics()) : AssistantError()
    data class ToolFailed(
        val toolName: String,
        val diagnostics: Diagnostics = Diagnostics(),
        val cause: Throwable? = null,
    ) : AssistantError()
    data class InvalidToolCall(
        val toolName: String,
        val diagnostics: Diagnostics = Diagnostics(),
    ) : AssistantError()
    data class OutcomeUnknown(
        val toolName: String,
        val diagnostics: Diagnostics = Diagnostics(),
    ) : AssistantError()
    data object Cancelled : AssistantError()
    data class Unknown(
        val cause: Throwable? = null,
        val diagnostics: Diagnostics = Diagnostics(),
    ) : AssistantError()
}
