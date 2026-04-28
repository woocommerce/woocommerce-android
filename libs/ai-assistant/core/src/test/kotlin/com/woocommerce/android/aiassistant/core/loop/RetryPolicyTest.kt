package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantErrorKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RetryPolicyTest {
    private val policy = ConservativeRetryPolicy

    @Test
    fun `given network error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.NETWORK, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given timeout error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.TIMEOUT, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given rate limit error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.RATE_LIMIT, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given network error after visible output, when deciding, then ShowManualRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.NETWORK, visibleOutputStarted = true, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.ShowManualRetry)
    }

    @Test
    fun `given auth error before visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.AUTH, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given unknown error before visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.UNKNOWN, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given network error and retry count at max, when deciding, then ShowManualRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(
                AssistantErrorKind.NETWORK,
                visibleOutputStarted = false,
                retryCount = ConservativeRetryPolicy.MAX_AUTO_RETRIES
            )
        )
        assertThat(decision).isEqualTo(RetryDecision.ShowManualRetry)
    }

    @Test
    fun `given RetryNow decision, when inspecting backoff, then positive backoff is set`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantErrorKind.NETWORK, visibleOutputStarted = false, retryCount = 0)
        )
        assertThat((decision as RetryDecision.RetryNow).backoffMs).isGreaterThan(0)
    }
}
