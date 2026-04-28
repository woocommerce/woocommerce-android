package com.woocommerce.android.aiassistant.core.chat

/**
 * Closed vocabulary for transport-level errors surfaced by [ChatService].
 * Transport-specific exceptions (IOException, HTTP status codes, JSON parse
 * failures) are normalized to one of these values at the service boundary so
 * the loop and UI speak a single error vocabulary.
 *
 * The full assistant error model (loop / tool / safety failures) lives
 * elsewhere; this enum only covers the failure modes a chat stream can
 * produce.
 */
enum class AssistantErrorKind {
    NETWORK,
    TIMEOUT,
    AUTH,
    RATE_LIMIT,
    UPSTREAM_FAILURE,
    INVALID_STREAM,
    CANCELLED,
    UNKNOWN,
}
