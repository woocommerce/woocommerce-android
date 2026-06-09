package com.woocommerce.android.aiassistant.core.chat

/**
 * Widen a transport-level [ChatStreamError] into the loop-level [AssistantError]
 * vocabulary. [INVALID_STREAM] is folded into [AssistantError.UpstreamFailure]
 * because there is no separate loop-level kind for malformed upstream bytes,
 * and the retry rules match. [cause] is only attached for [UNKNOWN] — every
 * other widening is total and lossless.
 */
fun ChatStreamError.toAssistantError(
    cause: Throwable? = null,
    diagnostics: Diagnostics = Diagnostics(),
): AssistantError = when (this) {
    ChatStreamError.NETWORK -> AssistantError.Network(diagnostics)
    ChatStreamError.TIMEOUT -> AssistantError.Timeout(diagnostics)
    ChatStreamError.AUTH -> AssistantError.Auth(diagnostics)
    ChatStreamError.RATE_LIMIT -> AssistantError.RateLimit(diagnostics)
    ChatStreamError.BAD_REQUEST -> AssistantError.BadRequest(diagnostics)
    ChatStreamError.UPSTREAM_FAILURE -> AssistantError.UpstreamFailure(diagnostics)
    ChatStreamError.INVALID_STREAM -> AssistantError.UpstreamFailure(diagnostics)
    ChatStreamError.CANCELLED -> AssistantError.Cancelled
    ChatStreamError.UNKNOWN -> AssistantError.Unknown(cause = cause, diagnostics = diagnostics)
}
