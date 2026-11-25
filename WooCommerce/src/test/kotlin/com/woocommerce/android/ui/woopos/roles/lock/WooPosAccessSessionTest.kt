package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosAccessSessionTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
    }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }

    @Test
    fun `when checking the Roles rollout default, then the local flag is off`() {
        assertThat(FeatureFlag.WOO_POS_ROLES.localValue).isFalse()
    }

    @Test
    fun `given flag off, when initialized and locked, then POS remains unprotected`() = runTest {
        // GIVEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(false)
        val session = session(fixture())

        // WHEN
        session.initialize()
        session.lock()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.isEnabled).isFalse()
    }

    @Test
    fun `given protection on without an operator, when initializing, then full lock is immediate`() = runTest {
        // GIVEN
        val session = session(fixture())
        assertThat(session.state.value.isLocked).isTrue()

        // WHEN
        session.initialize()

        // THEN
        assertThat((session.state.value.status == WooPosPINEntryStatus.Idle)).isTrue()
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.isEnabled).isTrue()
    }

    @Test
    fun `given no credentials, when initializing, then protection and manual lock are off`() = runTest {
        // GIVEN
        val fixture = fixture().apply { pins = Result.success(emptyMap()) }
        val session = session(fixture)

        // WHEN
        session.initialize()
        session.lock()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.isEnabled).isFalse()
    }

    @Test
    fun `given first PIN setup, when the new Admin unlocks, then launcher restoration is retained`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.pins
        fixture.pins = Result.success(emptyMap())
        val session = session(fixture)
        session.initialize()
        fixture.pins = configured

        // WHEN
        session.unlock("1234")

        // THEN
        org.mockito.kotlin.verify(fixture.lockPreferences).retainPOS(eq(1), any())
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
    }

    @Test
    fun `given source read failure, when initializing or entering PIN, then POS stays locked without charging`() = runTest {
        // GIVEN
        val fixture = fixture().apply { pins = Result.failure(IllegalStateException()) }
        val session = session(fixture)

        // WHEN
        session.initialize()
        session.unlock("1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.StorageUnavailable)
        assertThat(fixture.records).isEmpty()
    }

    @Test
    fun `when wrong then correct Cashier PINs are entered, then only correct configured PIN activates its permissions`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()

        // WHEN
        session.unlock("0000")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Incorrect)

        // WHEN
        session.unlock("5678")

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.state.value.activeRole).isEqualTo(fixture.cashier)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given an active Admin, when manual lock or process restart occurs, then authority is removed`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("1234")
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)

        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.isLocked).isTrue()
        val restarted = session(fixture)
        restarted.initialize()
        assertThat(restarted.state.value.isLocked).isTrue()
        assertThat(restarted.state.value.activeRole).isNull()
    }

    @Test
    fun `given wait, when PIN input or recovery is requested, then PIN is blocked and recovery preserves wait`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        repeat(5) { session.unlock("0000") }

        // WHEN
        session.unlock("1234")
        session.recover()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()

        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))
        assertThat(session.state.value.status).isNotEqualTo(WooPosPINEntryStatus.StorageUnavailable)
        fixture.elapsed += 1001
        session.refreshWait()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(29))
    }

    @Test
    fun `given active operator, when store changes or logs out, then authority cannot cross stores`() = runTest {
        // GIVEN
        val session = session(fixture())
        session.initialize()
        session.unlock("1234")

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given protection was off, when Admin PIN is configured, then no operator means full lock`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.pins
        fixture.pins = Result.success(emptyMap())
        val session = session(fixture)
        session.initialize()
        assertThat(session.state.value.isLocked).isFalse()

        // WHEN
        fixture.pins = configured
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given suspended unlock, when manual lock wins, then late success cannot reactivate the operator`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        val unlock = async { session.unlock("1234") }
        runCurrent()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Checking)

        // WHEN
        val lock = async { session.lock() }
        runCurrent()
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Checking)
        assertThat((session.state.value.status == WooPosPINEntryStatus.Idle)).isFalse()
        gate.complete(Unit)
        unlock.await()
        lock.await()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given suspended unlock, when logout resets the store, then late success cannot cross the session`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        val unlock = async { session.unlock("1234") }
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val reset = async { session.initialize() }
        gate.complete(Unit)
        unlock.await()
        reset.await()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given same-store refresh fails, when retried, then a valid PIN unlocks`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.pins
        val session = session(fixture)
        session.initialize()
        fixture.pins = Result.failure(IllegalStateException())

        // WHEN
        session.initialize()
        fixture.pins = configured
        session.initialize()
        session.unlock("1234")

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given no selected store, when four digits are entered, then the failed entry clears without unlock`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val session = session(fixture())
        session.initialize()

        // WHEN
        session.unlock("1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()

        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.StorageUnavailable)
    }

    @Test
    fun `given first PIN setup, when activating the saved PIN, then no intermediate lock is published`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.pins
        fixture.pins = Result.success(emptyMap())
        val session = session(fixture)
        session.initialize()
        val lockedStates = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.state.collect { lockedStates += it.isLocked }
        }
        fixture.pins = configured

        // WHEN
        session.unlock("1234")

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(session.isEnabled).isTrue()
        assertThat(lockedStates).containsOnly(false)
    }

    @Test
    fun `given first PIN setup with failed attempt persistence, when activating, then the session stays locked`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        fixture.writeError = true

        // WHEN
        session.unlock("1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.StorageUnavailable)
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `when an operator unlocks and locks, then the state flow exposes and removes the role`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        val roles = mutableListOf<WooPosLocalRole?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.state.map { it.activeRole }.distinctUntilChanged().collect { roles += it }
        }

        // WHEN
        session.unlock("5678")
        session.lock()

        // THEN
        assertThat(roles).containsExactly(null, fixture.cashier, null)
    }

    @Test
    fun `given pending recovery, when approval checks a PIN, then recovery does not hold the limiter`() = runTest {
        // GIVEN
        val fixture = fixture()
        val rateLimiter = fixture.rateLimiter()
        val session = session(fixture, rateLimiter)
        session.initialize()
        val gate = CompletableDeferred<Unit>()
        fixture.recoveryAction = {
            gate.await()
            Result.success(fixture.admin)
        }
        val recovery = async { session.recover() }
        runCurrent()

        // WHEN
        val result = rateLimiter.authenticate(1, "0000")

        // THEN
        assertThat(result).isEqualTo(WooPosLocalRateLimiterResult.Incorrect(0))
        assertThat(recovery.isCompleted).isFalse()
        gate.complete(Unit)
        recovery.await()
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given pending recovery, when cancelled, then attempts and the previous lock state remain`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        repeat(5) { session.unlock("0000") }
        val previousState = session.state.value
        val previousAttempts = fixture.records.toMap()
        val gate = CompletableDeferred<Unit>()
        fixture.recoveryAction = {
            gate.await()
            Result.success(fixture.admin)
        }
        val recovery = async { session.recover() }
        runCurrent()

        // WHEN
        recovery.cancelAndJoin()

        // THEN
        assertThat(recovery.isCancelled).isTrue()
        assertThat(session.state.value).isEqualTo(previousState)
        assertThat(fixture.records).isEqualTo(previousAttempts)
    }

    @Test
    fun `given failed or non-Admin recovery, when recovering, then no attempts are reset or role published`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("0000")
        val previous = fixture.records.toMap()
        fixture.recoveryAction = { error("Recovery unavailable") }

        // THEN
        session.recover()
        assertThat(session.state.value.activeRole).isNull()
        assertThat(fixture.records).isEqualTo(previous)

        // WHEN
        fixture.recoveryAction = { Result.success(fixture.cashier) }
        session.recover()

        // THEN
        assertThat(session.state.value.activeRole).isNull()
        assertThat(fixture.records).isEqualTo(previous)
    }

    @Test
    fun `given recovery reset fails, when the Admin is verified, then no role is published`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("0000")
        val previous = fixture.records.toMap()
        fixture.recoveryResult = Result.success(fixture.admin)
        fixture.writeError = true

        // WHEN
        session.recover()

        // THEN
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(fixture.records).isEqualTo(previous)
    }

    @Test
    fun `given pending recovery, when a manual lock invalidates it, then late success cannot reset or unlock`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("0000")
        val previous = fixture.records.toMap()
        val gate = CompletableDeferred<Unit>()
        fixture.recoveryAction = {
            gate.await()
            Result.success(fixture.admin)
        }
        val recovery = async { session.recover() }
        runCurrent()

        // WHEN
        session.lock()
        gate.complete(Unit)
        recovery.await()

        // THEN
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(fixture.records).isEqualTo(previous)
    }

    @Test
    fun `given exit is committing, when background locking runs, then the approved exit completes once`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("1234")
        val clear = CompletableDeferred<Result<Unit>>()
        whenever(fixture.lockPreferences.clearRestoreState(eq(1), any())).doSuspendableAnswer { clear.await() }
        var exits = 0
        val exiting = async { session.exitPOS { exits++ } }
        runCurrent()

        // WHEN
        session.lockOnBackground()
        session.exitPOS { exits++ }
        clear.complete(Result.success(Unit))
        exiting.await()

        // THEN
        assertThat(exits).isEqualTo(1)
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given exit is committing, when a saved PIN editor submits, then it cannot reactivate an operator`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        session.unlock("1234")
        val clear = CompletableDeferred<Result<Unit>>()
        whenever(fixture.lockPreferences.clearRestoreState(eq(1), any())).doSuspendableAnswer { clear.await() }
        val exiting = async { session.exitPOS {} }
        runCurrent()

        // WHEN
        session.unlock("1234")

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosAccessSessionState.Exiting)
        assertThat(session.state.value.activeRole).isNull()
        clear.complete(Result.success(Unit))
        exiting.await()
    }

    @Test
    fun `given logout occurs during exit storage, when cleanup returns, then stale navigation is rejected`() = runTest {
        // GIVEN
        val fixture = fixture()
        val limiter = fixture.rateLimiter()
        val session = session(fixture, limiter)
        session.initialize()
        session.unlock("1234")
        whenever(fixture.lockPreferences.clearRestoreState(eq(1), any())).doSuspendableAnswer {
            limiter.onLogout()
            Result.success(Unit)
        }
        var exited = false

        // WHEN
        session.exitPOS { exited = true }

        // THEN
        assertThat(exited).isFalse()
        assertThat(session.state.value.activeRole).isNull()
    }

    @Test
    fun `given no PIN protection, when stale marker cleanup fails during exit, then it cannot introduce a PIN lock`() = runTest {
        // GIVEN
        val fixture = fixture().apply { pins = Result.success(emptyMap()) }
        val session = session(fixture)
        session.initialize()
        whenever(
            fixture.lockPreferences.clearRestoreState(eq(1), any())
        ).thenReturn(Result.failure(IllegalStateException()))
        var exited = false

        // WHEN
        session.exitPOS { exited = true }

        // THEN
        assertThat(exited).isTrue()
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.status.canEnterPIN).isFalse()
    }

    private fun fixture() = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
    private fun session(
        fixture: WooPosLocalRateLimiterTestFixture,
        rateLimiter: WooPosLocalRateLimiter = fixture.rateLimiter(),
    ) = WooPosAccessSession(
        flags,
        selectedSite,
        rateLimiter,
        fixture.recovery,
        fixture.lockPreferences,
        fixture.cardReaderFacade,
    )
}
