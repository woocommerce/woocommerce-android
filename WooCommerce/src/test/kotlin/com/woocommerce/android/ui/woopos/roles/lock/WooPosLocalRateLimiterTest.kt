package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLocalRateLimiterTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    @Test
    fun `when failure thresholds are reached, then waits escalate and never permanently lock`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()

        // WHEN
        (1..30).forEach { count ->
            val result = rateLimiter.authenticate(1, "0000")
            val expected = when (count) {
                5 -> 30L
                10 -> 300L
                15 -> 1800L
                20, 25, 30 -> 3600L
                else -> 0L
            }

            // THEN
            assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(expected))
            if (expected > 0) {
                assertThat(rateLimiter.authenticate(1, "1234")).isEqualTo(WooPosLocalRateLimiterResult.Waiting(expected))
                fixture.elapsed += expected * 1000
            }
        }
    }

    @Test
    fun `given four failures, when Cashier unlocks then approval fails, then shared wait starts`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        repeat(4) { rateLimiter.authenticate(1, "0000") }

        // WHEN
        val cashier = rateLimiter.authenticate(1, "5678")
        val approval = rateLimiter.authenticate(1, "0000", WooPosCapability.ISSUE_REFUNDS)

        // THEN
        assertThat(cashier).isEqualTo(WooPosLocalRateLimiterResult.Authenticated(fixture.cashier))
        assertThat(approval).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(30))
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
    }

    @Test
    fun `given previous failures, when Admin unlocks, then attempt budget resets`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        repeat(4) { rateLimiter.authenticate(1, "0000") }

        // WHEN
        val result = rateLimiter.authenticate(1, "1234")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Authenticated(fixture.admin))
        assertThat(fixture.records.getValue(1).failures).isZero()
        assertThat(rateLimiter.authenticate(1, "0000")).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(0))
    }

    @Test
    fun `given Cashier PIN, when approval requires a missing permission, then attempt counts and never elevates`() = runTest {
        // GIVEN
        val fixture = fixture()

        // WHEN
        val result = fixture.rateLimiter().authenticate(1, "5678", WooPosCapability.ISSUE_REFUNDS)

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(0))
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given a wait, when service restarts during the same boot, then remaining monotonic wait survives`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        repeat(5) { rateLimiter.authenticate(1, "0000") }
        fixture.elapsed += 12_001

        // WHEN
        val restarted = fixture.rateLimiter()

        // THEN
        assertThat(restarted.remainingSeconds(1).getOrThrow()).isEqualTo(18)
        assertThat(restarted.authenticate(1, "1234")).isEqualTo(WooPosLocalRateLimiterResult.Waiting(18))
        assertThat(restarted.authenticate(2, "1234"))
            .isInstanceOf(WooPosLocalRateLimiterResult.Authenticated::class.java)
    }

    @Test
    fun `given changed boot or decreased uptime, when loading penalty, then full wait restarts durably`() = runTest {
        // GIVEN
        listOf(true, false).forEach { newBoot ->
            val fixture = fixture()
            val rateLimiter = fixture.rateLimiter()
            repeat(5) { rateLimiter.authenticate(1, "0000") }
            fixture.elapsed = 1
            if (newBoot) fixture.boot++

            // WHEN
            assertThat(fixture.rateLimiter().remainingSeconds(1).getOrThrow()).isEqualTo(30)
            fixture.elapsed += 1000

            // THEN
            assertThat(fixture.rateLimiter().remainingSeconds(1).getOrThrow()).isEqualTo(29)
            assertThat(fixture.records.getValue(1).lastFailureMillis).isEqualTo(1)
        }
    }

    @Test
    fun `given failures, when 24 hours pass without a wrong PIN, then quiet budget resets at the boundary`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        rateLimiter.authenticate(1, "0000")
        fixture.elapsed += WooPosPINAttemptState.QUIET_PERIOD - 1
        rateLimiter.remainingSeconds(1).getOrThrow()
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)

        // WHEN
        fixture.elapsed++
        rateLimiter.remainingSeconds(1).getOrThrow()

        // THEN
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given old failures, when another wrong PIN is entered, then the quiet period starts again`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        rateLimiter.authenticate(1, "0000")
        fixture.elapsed += WooPosPINAttemptState.QUIET_PERIOD - 1
        rateLimiter.authenticate(1, "0000")

        // WHEN
        fixture.elapsed++
        rateLimiter.remainingSeconds(1).getOrThrow()

        // THEN
        assertThat(fixture.records.getValue(1).failures).isEqualTo(2)
    }

    @Test
    fun `given source failure or empty credentials, when authenticating, then no wrong guess is charged`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        fixture.pins = Result.failure(IllegalStateException())

        // THEN
        assertThat(rateLimiter.authenticate(1, "0000")).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(rateLimiter.isProtectionEnabled(1).isFailure).isTrue()
        assertThat(fixture.records).isEmpty()

        // WHEN
        fixture.pins = Result.success(emptyMap())

        // THEN
        assertThat(rateLimiter.authenticate(1, "0000")).isEqualTo(WooPosLocalRateLimiterResult.ProtectionOff)
        assertThat(rateLimiter.isProtectionEnabled(1).getOrThrow()).isFalse()
        assertThat(fixture.records).isEmpty()
    }

    @Test
    fun `given unreadable limiter or unavailable boot identity, when authenticating, then rateLimiter fails closed`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.readError = true
        val rateLimiter = fixture.rateLimiter()

        // THEN
        assertThat(rateLimiter.authenticate(1, "1234")).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(rateLimiter.remainingSeconds(1).isFailure).isTrue()
        fixture.readError = false
        val badClock = object : WooPosPINClock {
            override fun now(): WooPosPINTime = error("Unavailable boot identity")
        }
        val clockFailure = WooPosLocalRateLimiter(
            fixture.store,
            WooPosPINAuthenticator(
                coroutineRule.testDispatchers
            ),
            fixture.storage,
            badClock,
            fixture.recovery,
            coroutineRule.testDispatchers,
        )
        assertThat(clockFailure.authenticate(1, "1234")).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(fixture.records).isEmpty()
    }

    @Test
    fun `given failed limiter writes, when correct Admin PIN is supplied, then no operator is published`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.writeError = true

        // THEN
        assertThat(fixture.rateLimiter().authenticate(1, "1234")).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(fixture.rateLimiter().authenticate(1, "0000")).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
    }

    @Test
    fun `given concurrent unlock and approval guesses, when threshold reached, then later guesses wait`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()

        // WHEN
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        val requests = (1..8).map { async { rateLimiter.authenticate(1, "0000", WooPosCapability.ISSUE_REFUNDS) } }
        runCurrent()
        assertThat(fixture.pinReads).isEqualTo(1)
        gate.complete(Unit)
        val results = requests.awaitAll()

        // THEN
        assertThat(results.filterIsInstance<WooPosLocalRateLimiterResult.Incorrect>()).hasSize(5)
        assertThat(results.filterIsInstance<WooPosLocalRateLimiterResult.Waiting>()).hasSize(3)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
    }

    @Test
    fun `given a wait, when recovery is unavailable then verified recovery succeeds, then only success resets`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        repeat(5) { rateLimiter.authenticate(1, "0000") }

        // WHEN
        assertThat(rateLimiter.recover(1)).isEqualTo(WooPosLocalRateLimiterResult.RecoveryUnavailable)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
        fixture.recoveryResult = Result.success(fixture.admin)

        // THEN
        assertThat(rateLimiter.recover(1)).isEqualTo(WooPosLocalRateLimiterResult.Authenticated(fixture.admin))
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given PINs change after precharge, when authenticating, then the same single snapshot is compared`() = runTest {
        // GIVEN
        val fixture = fixture()
        val writes = mutableListOf<Long>()
        fixture.onAttemptWrite = { state ->
            writes += state.failures
            fixture.pins = Result.success(emptyMap())
        }

        // WHEN
        val result = fixture.rateLimiter().authenticate(1, "1234")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Authenticated(fixture.admin))
        assertThat(fixture.pinReads).isEqualTo(1)
        assertThat(writes).containsExactly(1L, 0L)
    }

    @Test
    fun `given Admin reset cannot be saved, when the PIN matches, then no role is returned`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.onAttemptWrite = { state -> check(state.failures > 0) }

        // WHEN
        val result = fixture.rateLimiter().authenticate(1, "1234")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given comparison failure, when authenticating, then precharge is refunded without authority`() = runTest {
        // GIVEN
        val fixture = fixture()
        val previous = WooPosPINAttemptState(4, fixture.boot, fixture.elapsed, 0)
        fixture.records[1] = previous
        val authenticator: WooPosPINAuthenticator = mock()
        whenever(authenticator.authenticate(fixture.pins.getOrThrow(), "1234"))
            .thenReturn(Result.failure(IllegalStateException()))
        val rateLimiter = WooPosLocalRateLimiter(
            fixture.store,
            authenticator,
            fixture.storage,
            fixture.clock,
            fixture.recovery,
            coroutineRule.testDispatchers,
        )
        val writes = mutableListOf<WooPosPINAttemptState>()
        fixture.onAttemptWrite = { writes.add(it) }

        // WHEN
        val result = rateLimiter.authenticate(1, "1234")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(writes).containsExactly(previous.failed(WooPosPINTime(fixture.boot, fixture.elapsed)), previous)
        assertThat(fixture.records[1]).isEqualTo(previous)
    }

    @Test
    fun `given pending recovery, when a PIN is checked, then recovery does not block the check`() = runTest {
        // GIVEN
        val fixture = fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.recoveryAction = {
            gate.await()
            Result.success(fixture.admin)
        }
        val rateLimiter = fixture.rateLimiter()
        val recovery = async { rateLimiter.recover(1) }
        runCurrent()

        // WHEN
        val result = rateLimiter.authenticate(1, "0000")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(0))
        assertThat(recovery.isCompleted).isFalse()
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
        gate.complete(Unit)
        assertThat(recovery.await()).isEqualTo(WooPosLocalRateLimiterResult.Authenticated(fixture.admin))
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given pending recovery, when cancelled, then no reset is written`() = runTest {
        // GIVEN
        val fixture = fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.recoveryAction = {
            gate.await()
            Result.success(fixture.admin)
        }
        val rateLimiter = fixture.rateLimiter()
        rateLimiter.authenticate(1, "0000")
        val previous = fixture.records.toMap()
        val recovery = async { rateLimiter.recover(1) }
        runCurrent()

        // WHEN
        recovery.cancelAndJoin()

        // THEN
        assertThat(recovery.isCancelled).isTrue()
        assertThat(fixture.records).isEqualTo(previous)
    }

    @Test
    fun `given recovery throws or returns a non-Admin role, when recovering, then no reset is written`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        rateLimiter.authenticate(1, "0000")
        val previous = fixture.records.toMap()
        fixture.recoveryAction = { error("Recovery unavailable") }

        // THEN
        assertThat(rateLimiter.recover(1)).isEqualTo(WooPosLocalRateLimiterResult.RecoveryUnavailable)
        assertThat(fixture.records).isEqualTo(previous)

        // WHEN
        fixture.recoveryAction = { Result.success(fixture.cashier) }

        // THEN
        assertThat(rateLimiter.recover(1)).isEqualTo(WooPosLocalRateLimiterResult.RecoveryUnavailable)
        assertThat(fixture.records).isEqualTo(previous)
    }

    @Test
    fun `given recovery reset cannot be saved, when recovering, then no role is returned`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.recoveryResult = Result.success(fixture.admin)
        fixture.writeError = true

        // THEN
        assertThat(fixture.rateLimiter().recover(1)).isEqualTo(WooPosLocalRateLimiterResult.RecoveryUnavailable)
        assertThat(fixture.records).isEmpty()
    }

    private fun fixture() = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
}
