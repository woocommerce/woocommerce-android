package com.woocommerce.android.aiassistant.core.chat

/**
 * Closed vocabulary for transport-level errors surfaced by [ChatService].
 * Transport-specific exceptions (IOException, HTTP status codes, JSON parse
 * failures) are normalized to one of these values at the service boundary so
 * the loop layer only has to map this small set into [AssistantError].
 *
 * The full loop-level error model — including tool-execution variants
 * ([AssistantError.ToolFailed], [AssistantError.InvalidToolCall],
 * [AssistantError.OutcomeUnknown]) — lives in [AssistantError]. This enum
 * only covers the failure modes a chat stream can produce.
 */
enum class ChatStreamError {
    NETWORK,
    TIMEOUT,
    AUTH,
    RATE_LIMIT,
    BAD_REQUEST,
    UPSTREAM_FAILURE,
    INVALID_STREAM,
    CANCELLED,
    UNKNOWN,
}
