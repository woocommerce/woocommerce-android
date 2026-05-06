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
        else -> RetryDecision.RetryNow(BACKOFF_MS)
    }

    private fun isRetryable(error: AssistantError): Boolean = when (error) {
        AssistantError.Network,
        AssistantError.Timeout,
        AssistantError.RateLimit -> true
        else -> false
    }
}
