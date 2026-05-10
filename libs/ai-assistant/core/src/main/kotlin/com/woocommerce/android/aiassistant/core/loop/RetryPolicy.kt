package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError

data class LoopFailureContext(
    val error: AssistantError,
    val visibleOutputStarted: Boolean,
    val retryCount: Int,
)

sealed interface RetryDecision {
    data class RetryNow(val backoffMs: Long) : RetryDecision
    data object ShowManualRetry : RetryDecision
    data object DoNotRetry : RetryDecision
}

fun interface RetryPolicy {
    fun decide(failure: LoopFailureContext): RetryDecision
}

object ConservativeRetryPolicy : RetryPolicy {
    const val MAX_AUTO_RETRIES = 2
    private const val BACKOFF_MS = 500L

    override fun decide(failure: LoopFailureContext): RetryDecision = when {
        !isRetryable(failure.error) -> RetryDecision.DoNotRetry
        failure.visibleOutputStarted -> RetryDecision.ShowManualRetry
        failure.retryCount >= MAX_AUTO_RETRIES -> RetryDecision.ShowManualRetry
        else -> RetryDecision.RetryNow(failure.error.retryBackoffMs())
    }

    private fun isRetryable(error: AssistantError): Boolean = when (error) {
        is AssistantError.Network,
        is AssistantError.Timeout,
        is AssistantError.RateLimit -> true
        is AssistantError.Auth,
        is AssistantError.BadRequest,
        is AssistantError.UpstreamFailure,
        is AssistantError.ToolFailed,
        is AssistantError.InvalidToolCall,
        is AssistantError.OutcomeUnknown,
        AssistantError.Cancelled,
        is AssistantError.Unknown -> false
    }

    private fun AssistantError.retryBackoffMs(): Long =
        (this as? AssistantError.RateLimit)?.diagnostics?.transport?.retryAfterMs ?: BACKOFF_MS
}
