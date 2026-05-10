package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RetryPolicyTest {
    private val policy = ConservativeRetryPolicy

    @Test
    fun `given network error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Network(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given timeout error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Timeout(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given rate limit error before visible output, when deciding, then RetryNow is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.RateLimit(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isInstanceOf(RetryDecision.RetryNow::class.java)
    }

    @Test
    fun `given rate limit with retry after, when deciding, then RetryNow uses diagnostic delay`() {
        val decision = policy.decide(
            LoopFailureContext(
                AssistantError.RateLimit(
                    diagnostics = Diagnostics(transport = TransportDiagnostics(retryAfterMs = 12_000L)),
                ),
                visibleOutputStarted = false,
                retryCount = 0,
            )
        )

        assertThat((decision as RetryDecision.RetryNow).backoffMs).isEqualTo(12_000L)
    }

    @Test
    fun `given rate limit without retry after, when deciding, then RetryNow uses default delay`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.RateLimit(), visibleOutputStarted = false, retryCount = 0)
        )

        assertThat((decision as RetryDecision.RetryNow).backoffMs).isEqualTo(500L)
    }

    @Test
    fun `given network error after visible output, when deciding, then ShowManualRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Network(), visibleOutputStarted = true, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.ShowManualRetry)
    }

    @Test
    fun `given auth error before visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Auth(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given unknown error before visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Unknown(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given upstream failure before visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.UpstreamFailure(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given BadRequest when deciding retry then does not retry`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.BadRequest(), visibleOutputStarted = false, retryCount = 0)
        )

        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given upstream failure after visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.UpstreamFailure(), visibleOutputStarted = true, retryCount = 0)
        )

        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given outcome unknown after visible output, when deciding, then DoNotRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(
                AssistantError.OutcomeUnknown(toolName = "orders_update"),
                visibleOutputStarted = true,
                retryCount = 0,
            )
        )

        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `given network error and retry count at max, when deciding, then ShowManualRetry is returned`() {
        val decision = policy.decide(
            LoopFailureContext(
                AssistantError.Network(),
                visibleOutputStarted = false,
                retryCount = ConservativeRetryPolicy.MAX_AUTO_RETRIES
            )
        )
        assertThat(decision).isEqualTo(RetryDecision.ShowManualRetry)
    }

    @Test
    fun `given RetryNow decision, when inspecting backoff, then positive backoff is set`() {
        val decision = policy.decide(
            LoopFailureContext(AssistantError.Network(), visibleOutputStarted = false, retryCount = 0)
        )
        assertThat((decision as RetryDecision.RetryNow).backoffMs).isGreaterThan(0)
    }
}
