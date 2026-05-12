package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollUntilTerminalTest : BaseUnitTest() {

    private val outcomes = ArrayDeque<PollOutcome>()
    private var pollCallCount = 0

    private val poll: suspend () -> PollOutcome = {
        pollCallCount++
        outcomes.removeFirstOrNull() ?: error("test ran out of stubbed outcomes")
    }

    @Test
    fun `given terminal outcome on first tick, when invoked, then returns it without delay`() = testBlocking {
        outcomes += PollOutcome.Approved("grant-1")
        val start = currentTime

        val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

        assertThat(result).isEqualTo(PollOutcome.Approved("grant-1"))
        assertThat(pollCallCount).isEqualTo(1)
        assertThat(currentTime - start).isEqualTo(0)
    }

    @Test
    fun `given Scanned then Expired, when invoked, then the gap between ticks equals POLL_INTERVAL_MS`() =
        testBlocking {
            outcomes += PollOutcome.Scanned
            outcomes += PollOutcome.Expired
            val start = currentTime

            val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

            assertThat(result).isEqualTo(PollOutcome.Expired)
            assertThat(pollCallCount).isEqualTo(2)
            assertThat(currentTime - start).isEqualTo(POLL_INTERVAL_MS)
        }

    @Test
    fun `given shouldContinue is false from the start, when invoked, then returns null without polling`() =
        testBlocking {
            val result = pollUntilTerminal(shouldContinue = { false }, poll = poll)

            assertThat(result).isNull()
            assertThat(pollCallCount).isEqualTo(0)
        }

    @Test
    fun `given shouldContinue flips false during poll, when poll returns, then outcome is dropped and returns null`() =
        testBlocking {
            var active = true
            outcomes += PollOutcome.Approved("grant-1")
            val flippingPoll: suspend () -> PollOutcome = {
                pollCallCount++
                active = false
                outcomes.removeFirst()
            }

            val result = pollUntilTerminal(shouldContinue = { active }, poll = flippingPoll)

            assertThat(result).isNull()
            assertThat(pollCallCount).isEqualTo(1)
        }

    @Test
    fun `given Rejected outcome, when invoked, then returns immediately`() = testBlocking {
        outcomes += PollOutcome.Rejected

        val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

        assertThat(result).isEqualTo(PollOutcome.Rejected)
    }

    @Test
    fun `given AlreadyCompleted outcome, when invoked, then returns immediately`() = testBlocking {
        outcomes += PollOutcome.AlreadyCompleted

        val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

        assertThat(result).isEqualTo(PollOutcome.AlreadyCompleted)
    }

    @Test
    fun `given MAX_CONSECUTIVE_POLL_ERRORS non-terminal transient errors, when invoked, then returns the last one`() =
        testBlocking {
            val transient = transientError()
            repeat(MAX_CONSECUTIVE_POLL_ERRORS) { outcomes += transient }

            val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

            assertThat(result).isEqualTo(transient)
            assertThat(pollCallCount).isEqualTo(MAX_CONSECUTIVE_POLL_ERRORS)
        }

    @Test
    fun `given fewer transient errors than threshold then Approved, when invoked, then returns Approved`() =
        testBlocking {
            repeat(MAX_CONSECUTIVE_POLL_ERRORS - 1) { outcomes += transientError() }
            outcomes += PollOutcome.Approved("grant-1")

            val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

            assertThat(result).isEqualTo(PollOutcome.Approved("grant-1"))
            assertThat(pollCallCount).isEqualTo(MAX_CONSECUTIVE_POLL_ERRORS)
        }

    @Test
    fun `given transient errors with a Scanned in the middle, when invoked, then Scanned resets the counter`() =
        testBlocking {
            // 3 errors, then Scanned (resets counter), then 3 more errors, then Approved.
            // Without the reset this would trip the threshold on the 4th total error.
            repeat(MAX_CONSECUTIVE_POLL_ERRORS - 1) { outcomes += transientError() }
            outcomes += PollOutcome.Scanned
            repeat(MAX_CONSECUTIVE_POLL_ERRORS - 1) { outcomes += transientError() }
            outcomes += PollOutcome.Approved("grant-1")

            val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

            assertThat(result).isEqualTo(PollOutcome.Approved("grant-1"))
        }

    @Test
    fun `given a TransientError marked terminal, when invoked, then returns it on the first tick`() = testBlocking {
        val terminal = PollOutcome.TransientError(
            reason = ErrorReason.RateLimited,
            terminal = true,
            cause = RuntimeException("429"),
        )
        outcomes += terminal

        val result = pollUntilTerminal(shouldContinue = { true }, poll = poll)

        assertThat(result).isEqualTo(terminal)
        assertThat(pollCallCount).isEqualTo(1)
    }

    private fun transientError(): PollOutcome.TransientError = PollOutcome.TransientError(
        reason = ErrorReason.Network,
        terminal = false,
        cause = RuntimeException("network"),
    )
}
