package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.roles.WooPosPermission
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPINAccessTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    @Test
    fun `when failure thresholds are reached, then waits escalate and never permanently lock`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()

        // WHEN
        (1..30).forEach { count ->
            val result = access.authenticate(1, "0000")
            val expected = when (count) {
                5 -> 30L
                10 -> 300L
                15 -> 1800L
                20, 25, 30 -> 3600L
                else -> 0L
            }

            // THEN
            assertThat(result).isEqualTo(WooPosPINAccessResult.Incorrect(expected))
            if (expected > 0) {
                assertThat(access.authenticate(1, "1234")).isEqualTo(WooPosPINAccessResult.Waiting(expected))
                fixture.elapsed += expected * 1000
            }
        }
    }

    @Test
    fun `given four failures, when Cashier unlocks then approval fails, then shared wait starts`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        repeat(4) { access.authenticate(1, "0000") }

        // WHEN
        val cashier = access.authenticate(1, "5678")
        val approval = access.authenticate(1, "0000", WooPosPermission.ISSUE_REFUNDS)

        // THEN
        assertThat(cashier).isEqualTo(WooPosPINAccessResult.Authenticated(fixture.cashier))
        assertThat(approval).isEqualTo(WooPosPINAccessResult.Incorrect(30))
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
    }

    @Test
    fun `given previous failures, when Admin unlocks, then attempt budget resets`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        repeat(4) { access.authenticate(1, "0000") }

        // WHEN
        val result = access.authenticate(1, "1234")

        // THEN
        assertThat(result).isEqualTo(WooPosPINAccessResult.Authenticated(fixture.admin))
        assertThat(fixture.records.getValue(1).failures).isZero()
        assertThat(access.authenticate(1, "0000")).isEqualTo(WooPosPINAccessResult.Incorrect(0))
    }

    @Test
    fun `given Cashier PIN, when approval requires a missing permission, then attempt counts and never elevates`() = runTest {
        // GIVEN
        val fixture = fixture()

        // WHEN
        val result = fixture.access().authenticate(1, "5678", WooPosPermission.ISSUE_REFUNDS)

        // THEN
        assertThat(result).isEqualTo(WooPosPINAccessResult.Incorrect(0))
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given a wait, when service restarts during the same boot, then remaining monotonic wait survives`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        repeat(5) { access.authenticate(1, "0000") }
        fixture.elapsed += 12_001

        // WHEN
        val restarted = fixture.access()

        // THEN
        assertThat(restarted.remainingSeconds(1).getOrThrow()).isEqualTo(18)
        assertThat(restarted.authenticate(1, "1234")).isEqualTo(WooPosPINAccessResult.Waiting(18))
        assertThat(restarted.authenticate(2, "1234")).isInstanceOf(WooPosPINAccessResult.Authenticated::class.java)
    }

    @Test
    fun `given changed boot or decreased uptime, when loading penalty, then full wait restarts durably`() = runTest {
        // GIVEN
        listOf(true, false).forEach { newBoot ->
            val fixture = fixture()
            val access = fixture.access()
            repeat(5) { access.authenticate(1, "0000") }
            fixture.elapsed = 1
            if (newBoot) fixture.boot++

            // WHEN
            assertThat(fixture.access().remainingSeconds(1).getOrThrow()).isEqualTo(30)
            fixture.elapsed += 1000

            // THEN
            assertThat(fixture.access().remainingSeconds(1).getOrThrow()).isEqualTo(29)
            assertThat(fixture.records.getValue(1).lastFailureMillis).isEqualTo(1)
        }
    }

    @Test
    fun `given failures, when 24 hours pass without a wrong PIN, then quiet budget resets at the boundary`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        access.authenticate(1, "0000")
        fixture.elapsed += WooPosPINAttemptState.QUIET_PERIOD - 1
        access.remainingSeconds(1).getOrThrow()
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)

        // WHEN
        fixture.elapsed++
        access.remainingSeconds(1).getOrThrow()

        // THEN
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given old failures, when another wrong PIN is entered, then the quiet period starts again`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        access.authenticate(1, "0000")
        fixture.elapsed += WooPosPINAttemptState.QUIET_PERIOD - 1
        access.authenticate(1, "0000")

        // WHEN
        fixture.elapsed++
        access.remainingSeconds(1).getOrThrow()

        // THEN
        assertThat(fixture.records.getValue(1).failures).isEqualTo(2)
    }

    @Test
    fun `given source failure or empty credentials, when authenticating, then no wrong guess is charged`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        fixture.credentials = Result.failure(IllegalStateException())

        // THEN
        assertThat(access.authenticate(1, "0000")).isEqualTo(WooPosPINAccessResult.Unavailable)
        assertThat(access.protection(1).isFailure).isTrue()
        assertThat(fixture.records).isEmpty()

        // WHEN
        fixture.credentials = Result.success(emptyList())

        // THEN
        assertThat(access.authenticate(1, "0000")).isEqualTo(WooPosPINAccessResult.ProtectionOff)
        assertThat(access.protection(1).getOrThrow()).isFalse()
        assertThat(fixture.records).isEmpty()
    }

    @Test
    fun `given unreadable limiter or unavailable boot identity, when authenticating, then access fails closed`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.readError = true
        val access = fixture.access()

        // THEN
        assertThat(access.authenticate(1, "1234")).isEqualTo(WooPosPINAccessResult.Unavailable)
        assertThat(access.remainingSeconds(1).isFailure).isTrue()
        val badClock = object : WooPosPINClock {
            override fun now(): WooPosPINTime = error("Unavailable boot identity")
        }
        val clockFailure = WooPosPINAccess(
            fixture.source,
            com.woocommerce.android.ui.woopos.roles.WooPosPINAuthenticator(
                fixture.source,
                coroutineRule.testDispatchers
            ),
            fixture.storage,
            badClock,
            fixture.recovery,
            coroutineRule.testDispatchers,
        )
        assertThat(clockFailure.authenticate(1, "1234")).isEqualTo(WooPosPINAccessResult.Unavailable)
    }

    @Test
    fun `given failed limiter writes, when correct Admin PIN is supplied, then no operator is published`() = runTest {
        // GIVEN
        val fixture = fixture()
        fixture.writeError = true

        // THEN
        assertThat(fixture.access().authenticate(1, "1234")).isEqualTo(WooPosPINAccessResult.Unavailable)
        assertThat(fixture.access().authenticate(1, "0000")).isEqualTo(WooPosPINAccessResult.Unavailable)
    }

    @Test
    fun `given concurrent unlock and approval guesses, when threshold reached, then later guesses wait`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()

        // WHEN
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val requests = (1..8).map { async { access.authenticate(1, "0000", WooPosPermission.ISSUE_REFUNDS) } }
        runCurrent()
        assertThat(fixture.credentialReads).isEqualTo(1)
        gate.complete(Unit)
        val results = requests.awaitAll()

        // THEN
        assertThat(results.filterIsInstance<WooPosPINAccessResult.Incorrect>()).hasSize(5)
        assertThat(results.filterIsInstance<WooPosPINAccessResult.Waiting>()).hasSize(3)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
    }

    @Test
    fun `given a wait, when recovery is unavailable then verified recovery succeeds, then only success resets`() = runTest {
        // GIVEN
        val fixture = fixture()
        val access = fixture.access()
        repeat(5) { access.authenticate(1, "0000") }

        // WHEN
        assertThat(access.recover(1)).isEqualTo(WooPosPINAccessResult.RecoveryUnavailable)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(5)
        fixture.recoveryResult = Result.success(fixture.admin)

        // THEN
        assertThat(access.recover(1)).isEqualTo(WooPosPINAccessResult.Authenticated(fixture.admin))
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    private fun fixture() = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
}
