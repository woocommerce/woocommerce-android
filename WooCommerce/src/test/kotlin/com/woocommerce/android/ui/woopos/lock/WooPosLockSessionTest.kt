package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLockSessionTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK) }.thenReturn(true)
    }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }

    @Test
    fun `given flag off, when initialized and locked, then POS remains unprotected`() = runTest {
        // GIVEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK)).thenReturn(false)
        val session = session(fixture())

        // WHEN
        session.initialize()
        session.lock()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.isEnabled).isFalse()
        assertThat(FeatureFlag.WOO_POS_MANUAL_LOCK.localValue).isFalse()
    }

    @Test
    fun `given protection on without an operator, when initializing, then full lock is immediate`() = runTest {
        // GIVEN
        val session = session(fixture())
        assertThat(session.state.value.isLocked).isTrue()

        // WHEN
        session.initialize()

        // THEN
        assertThat(session.state.value.canEnterPIN).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.isEnabled).isTrue()
    }

    @Test
    fun `given no credentials, when initializing, then protection and manual lock are off`() = runTest {
        // GIVEN
        val fixture = fixture().apply { credentials = Result.success(emptyList()) }
        val session = session(fixture)

        // WHEN
        session.initialize()
        session.lock()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.isEnabled).isFalse()
    }

    @Test
    fun `given source read failure, when initializing or entering PIN, then POS stays locked without charging`() = runTest {
        // GIVEN
        val fixture = fixture().apply { credentials = Result.failure(IllegalStateException()) }
        val session = session(fixture)

        // WHEN
        session.initialize()
        enter(session, "1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
        assertThat(fixture.records).isEmpty()
    }

    @Test
    fun `when wrong then correct Cashier PINs are entered, then only correct configured PIN activates its permissions`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()

        // WHEN
        enter(session, "0000")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.INCORRECT)
        assertThat(session.state.value.enteredDigits).isZero()

        // WHEN
        enter(session, "5678")

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.state.value.activeOperator).isEqualTo(fixture.cashier)
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given an active Admin, when manual lock or process restart occurs, then authority is removed`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        enter(session, "1234")
        assertThat(session.state.value.activeOperator).isEqualTo(fixture.admin)

        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.state.value.isLocked).isTrue()
        val restarted = session(fixture)
        restarted.initialize()
        assertThat(restarted.state.value.isLocked).isTrue()
        assertThat(restarted.state.value.activeOperator).isNull()
    }

    @Test
    fun `given partial entry, when deleting or receiving invalid digits, then only valid remaining entry is kept`() = runTest {
        // GIVEN
        val session = session(fixture())
        session.initialize()
        session.enterDigit(9)
        session.deleteDigit()
        session.deleteDigit()
        session.enterDigit(-1)
        session.enterDigit(10)

        // WHEN
        enter(session, "123")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.enteredDigits).isEqualTo(3)
        session.enterDigit(4)
        assertThat(session.state.value.isLocked).isFalse()
    }

    @Test
    fun `given wait, when PIN input or recovery is requested, then PIN is blocked and recovery preserves wait`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        repeat(5) { enter(session, "0000") }

        // WHEN
        enter(session, "1234")
        session.recover()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.enteredDigits).isZero()
        assertThat(session.state.value.remainingSeconds).isEqualTo(30)
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.RECOVERY_UNAVAILABLE)
        fixture.elapsed += 1001
        session.refreshWait()
        assertThat(session.state.value.remainingSeconds).isEqualTo(29)
    }

    @Test
    fun `given active operator, when store changes or logs out, then authority cannot cross stores`() = runTest {
        // GIVEN
        val session = session(fixture())
        session.initialize()
        enter(session, "1234")

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
    }

    @Test
    fun `given protection was off, when Admin PIN is configured, then no operator means full lock`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.credentials
        fixture.credentials = Result.success(emptyList())
        val session = session(fixture)
        session.initialize()
        assertThat(session.state.value.isLocked).isFalse()

        // WHEN
        fixture.credentials = configured
        session.initialize()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
    }

    @Test
    fun `given suspended unlock, when manual lock wins, then late success cannot reactivate the operator`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val unlock = async { enter(session, "1234") }
        runCurrent()
        assertThat(session.state.value.isChecking).isTrue()

        // WHEN
        session.lock()
        gate.complete(Unit)
        unlock.await()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.state.value.enteredDigits).isZero()
    }

    @Test
    fun `given suspended unlock, when logout resets the store, then late success cannot cross the session`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val unlock = async { enter(session, "1234") }
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val reset = async { session.initialize() }
        gate.complete(Unit)
        unlock.await()
        reset.await()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
    }

    @Test
    fun `given partial entry, when same-store refresh fails, then retry uses only the newly entered PIN`() = runTest {
        // GIVEN
        val fixture = fixture()
        val configured = fixture.credentials
        val session = session(fixture)
        session.initialize()
        enter(session, "12")
        fixture.credentials = Result.failure(IllegalStateException())

        // WHEN
        session.initialize()
        fixture.credentials = configured
        session.initialize()
        enter(session, "1234")

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.state.value.activeOperator).isEqualTo(fixture.admin)
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given no selected store, when four digits are entered, then the failed entry clears without unlock`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)
        val session = session(fixture())
        session.initialize()

        // WHEN
        enter(session, "1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.enteredDigits).isZero()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
    }

    @Test
    fun `given protected POS, when unlocked then manually locked, then durable marker remains until explicit exit`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        assertThat(fixture.markedStores).contains(1)
        enter(session, "1234")

        // WHEN
        session.lock()

        // THEN
        assertThat(fixture.markedStores).contains(1)
        assertThat(session.exitPOS()).isFalse()
        enter(session, "1234")
        assertThat(session.exitPOS()).isTrue()
        assertThat(fixture.markedStores).isEmpty()
    }

    @Test
    fun `given marker persistence failure, when correct PIN is supplied, then no unlocked authority is published`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markerError = true }
        val session = session(fixture)
        session.initialize()

        // WHEN
        enter(session, "1234")

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
    }

    @Test
    fun `given suspended refresh, when explicit exit waits then succeeds, then no late refresh can recreate the marker`() = runTest {
        // GIVEN
        val fixture = fixture()
        val session = session(fixture)
        session.initialize()
        enter(session, "1234")
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val refresh = async { session.initialize() }
        runCurrent()
        val exit = async { session.exitPOS() }
        runCurrent()
        assertThat(exit.isCompleted).isFalse()

        // WHEN
        gate.complete(Unit)
        refresh.await()
        assertThat(exit.await()).isTrue()
        session.initialize()
        session.lock()

        // THEN
        assertThat(fixture.markedStores).isEmpty()
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.state.value.isLocked).isFalse()
    }

    private fun fixture() = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
    private fun session(fixture: WooPosPINAccessTestFixture) = WooPosLockSession(
        flags,
        selectedSite,
        fixture.access(),
        fixture.lockedPOS
    )
    private suspend fun enter(session: WooPosLockSession, pin: String) {
        pin.forEach { session.enterDigit(it.digitToInt()) }
    }
}
