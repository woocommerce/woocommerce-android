package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.ChatStreamError

data class LoopFailureContext(
    val errorKind: ChatStreamError,
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

    private val retryableKinds = setOf(
        ChatStreamError.NETWORK,
        ChatStreamError.TIMEOUT,
        ChatStreamError.RATE_LIMIT,
    )

    override fun decide(failure: LoopFailureContext): RetryDecision = when {
        failure.visibleOutputStarted -> RetryDecision.ShowManualRetry
        failure.errorKind !in retryableKinds -> RetryDecision.DoNotRetry
        failure.retryCount >= MAX_AUTO_RETRIES -> RetryDecision.ShowManualRetry
        else -> RetryDecision.RetryNow(BACKOFF_MS)
    }
}
