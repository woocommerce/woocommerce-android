package com.woocommerce.android.aiassistant.core.chat

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Normalize a raw [Throwable] into the loop-level [AssistantError] vocabulary.
 *
 * Order matters: specific subtypes must be matched before their supertypes.
 * [SocketTimeoutException] is a subtype of [IOException] so it appears first.
 * [CancellationException] is preserved as [AssistantError.Cancelled] rather
 * than rethrown — callers that need to honor structured concurrency should
 * rethrow it before delegating to this helper.
 */
fun Throwable.toAssistantError(): AssistantError = when (this) {
    is AssistantAuthException -> AssistantError.Auth
    is CancellationException -> AssistantError.Cancelled
    is SocketTimeoutException -> AssistantError.Timeout
    is UnknownHostException -> AssistantError.Network
    is ConnectException -> AssistantError.Network
    is IOException -> AssistantError.Network
    else -> AssistantError.Unknown(cause = this)
}

/**
 * Normalize an HTTP status [code] into the loop-level [AssistantError]
 * vocabulary. Codes outside the documented ranges fall back to
 * [AssistantError.Unknown] with no cause attached.
 */
fun assistantErrorFromHttpCode(code: Int): AssistantError = when (code) {
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> AssistantError.Auth
    HTTP_REQUEST_TIMEOUT -> AssistantError.Timeout
    HTTP_TOO_MANY_REQUESTS -> AssistantError.RateLimit
    in HTTP_SERVER_ERROR_RANGE -> AssistantError.UpstreamFailure
    else -> AssistantError.Unknown()
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERROR_RANGE = 500..599

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
    ChatStreamError.UPSTREAM_FAILURE -> AssistantError.UpstreamFailure
    ChatStreamError.INVALID_STREAM -> AssistantError.UpstreamFailure
    ChatStreamError.CANCELLED -> AssistantError.Cancelled
    ChatStreamError.UNKNOWN -> AssistantError.Unknown(cause = cause)
}
