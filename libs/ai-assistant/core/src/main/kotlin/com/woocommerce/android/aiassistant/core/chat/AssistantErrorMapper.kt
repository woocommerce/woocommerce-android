package com.woocommerce.android.aiassistant.core.chat

/**
 * Widen a transport-level [ChatStreamError] into the loop-level [AssistantError]
 * vocabulary. [INVALID_STREAM] is folded into [AssistantError.UpstreamFailure]
 * because there is no separate loop-level kind for malformed upstream bytes,
 * and the retry rules match. [cause] is only attached for [UNKNOWN] — every
 * other widening is total and lossless.
 */
fun ChatStreamError.toAssistantError(cause: Throwable? = null): AssistantError = when (this) {
    ChatStreamError.NETWORK -> AssistantError.Network
    ChatStreamError.TIMEOUT -> AssistantError.Timeout
    ChatStreamError.AUTH -> AssistantError.Auth
    ChatStreamError.RATE_LIMIT -> AssistantError.RateLimit
    ChatStreamError.BAD_REQUEST -> AssistantError.BadRequest
    ChatStreamError.UPSTREAM_FAILURE -> AssistantError.UpstreamFailure
    ChatStreamError.INVALID_STREAM -> AssistantError.UpstreamFailure
    ChatStreamError.CANCELLED -> AssistantError.Cancelled
    ChatStreamError.UNKNOWN -> AssistantError.Unknown(cause = cause)
}
