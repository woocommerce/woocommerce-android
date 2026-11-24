package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLockScreenViewModelTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    @Test
    fun `given no PIN protection, when startup read is pending, then loading ends without PIN checking`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers).apply {
            pins = Result.success(emptyMap())
            pinReadGate = CompletableDeferred()
        }
        val (viewModel, _) = viewModel(fixture)

        // THEN
        assertThat(viewModel.state.value.isLocked).isTrue()
        assertThat(viewModel.state.value.canEnterPIN).isFalse()
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Initializing)

        // WHEN
        fixture.pinReadGate?.complete(Unit)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.isLocked).isFalse()
    }

    @Test
    fun `given configured PINs, when startup read completes, then loading becomes PIN entry`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers).apply {
            pinReadGate = CompletableDeferred()
        }
        val (viewModel, _) = viewModel(fixture)
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Initializing)
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(1))
        assertThat(viewModel.state.value.enteredDigits).isZero()

        // WHEN
        fixture.pinReadGate?.complete(Unit)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.isLocked).isTrue()
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Idle)
        assertThat(viewModel.state.value.canEnterPIN).isTrue()
    }

    @Test
    fun `given failed startup read, when loading ends, then POS stays blocked with unavailable state`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers).apply {
            pins = Result.failure(IllegalStateException())
            pinReadGate = CompletableDeferred()
        }
        val (viewModel, _) = viewModel(fixture)
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Initializing)

        // WHEN
        fixture.pinReadGate?.complete(Unit)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.isLocked).isTrue()
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.StorageUnavailable)
        assertThat(viewModel.state.value.canEnterPIN).isFalse()
    }

    @Test
    fun `given wrong entries, when wait starts, then countdown updates live until PIN entry returns`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val site = SiteModel().apply { id = 1 }
        val selectedSite: SelectedSite = mock {
            on { getOrNull() }.thenReturn(site)
            on { observe() }.thenReturn(MutableStateFlow(site))
        }
        val flags: FeatureFlagRepository = mock {
            on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
        }
        val viewModel = WooPosLockScreenViewModel(
            WooPosAccessSession(flags, selectedSite, fixture.rateLimiter(), fixture.recovery, fixture.lockPreferences, fixture.cardReaderFacade),
            selectedSite,
            fixture.lockPreferences,
            fixture.elapsedTime,
        )
        repeat(5) {
            repeat(4) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(0)) }
        }
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))

        // WHEN
        val readsBeforeTick = fixture.attemptReads
        fixture.elapsed += 1000
        advanceTimeBy(999)
        runCurrent()
        assertThat(fixture.attemptReads).isEqualTo(readsBeforeTick)
        advanceTimeBy(1)
        runCurrent()
        assertThat(fixture.attemptReads).isEqualTo(readsBeforeTick + 1)

        // THEN
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(29))
        assertThat(viewModel.state.value.isLocked).isTrue()

        // WHEN
        fixture.elapsed += 29_000
        advanceTimeBy(29_000)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Idle)
        assertThat(viewModel.state.value.canEnterPIN).isTrue()
    }

    @Test
    fun `given shared approval wait while unlocked, when manually locked, then countdown appears without PIN entry`() =
        runTest {
            // GIVEN
            val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
            val site = SiteModel().apply { id = 1 }
            val selectedSite: SelectedSite = mock {
                on { getOrNull() }.thenReturn(site)
                on { observe() }.thenReturn(MutableStateFlow(site))
            }
            val flags: FeatureFlagRepository = mock {
                on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
            }
            val access = fixture.rateLimiter()
            val session = WooPosAccessSession(flags, selectedSite, access, fixture.recovery, fixture.lockPreferences, fixture.cardReaderFacade)
            val viewModel = WooPosLockScreenViewModel(session, selectedSite, fixture.lockPreferences, fixture.elapsedTime)
            session.unlock("5678")
            assertThat(session.state.value.activeRole).isEqualTo(fixture.cashier)
            repeat(5) { access.authenticate(1, "0000", WooPosCapability.ISSUE_REFUNDS) }
            val attempts = fixture.records.toMap()

            // WHEN
            session.lock()
            runCurrent()

            // THEN
            assertThat(viewModel.state.value.isLocked).isTrue()
            assertThat(session.state.value.activeRole).isNull()
            assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))
            assertThat(viewModel.state.value.canEnterPIN).isFalse()
            repeat(4) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(0)) }
            assertThat(viewModel.state.value.enteredDigits).isZero()
            assertThat(fixture.records).isEqualTo(attempts)
            assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))

            // WHEN
            fixture.elapsed += 30_000
            advanceTimeBy(30_000)
            runCurrent()

            // THEN
            assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Idle)
            assertThat(viewModel.state.value.canEnterPIN).isTrue()
        }

    @Test
    fun `given partial entry, when deleting or receiving invalid digits, then only valid remaining entry is kept`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val (viewModel, session) = viewModel(fixture)

        // WHEN
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(9))
        repeat(2) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DeleteTapped) }
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(-1))
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(10))
        "123".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }

        // THEN
        assertThat(viewModel.state.value.enteredDigits).isEqualTo(3)
        assertThat(session.state.value.activeRole).isNull()
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(4))
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
    }

    @Test
    fun `given partial entry, when protection refresh fails then succeeds, then old digits are discarded`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val configured = fixture.pins
        val (viewModel, session) = viewModel(fixture)
        "12".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }

        // WHEN
        fixture.pins = Result.failure(IllegalStateException())
        session.initialize()
        fixture.pins = configured
        session.initialize()
        "1234".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(viewModel.state.value.enteredDigits).isZero()
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given rejected PIN verification, when checking completes, then four dots become an empty incorrect entry`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val (viewModel, _) = viewModel(fixture)
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        val readsBeforeSubmission = fixture.pinReads

        // WHEN
        repeat(4) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(0)) }
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Checking)
        assertThat(viewModel.state.value.enteredDigits).isEqualTo(4)
        assertThat(viewModel.state.value.canEnterPIN).isFalse()
        assertThat(fixture.pinReads).isEqualTo(readsBeforeSubmission + 1)

        // WHEN
        gate.complete(Unit)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Incorrect)
        assertThat(viewModel.state.value.enteredDigits).isZero()
        assertThat(viewModel.state.value.canEnterPIN).isTrue()
        assertThat(fixture.records.getValue(1).failures).isEqualTo(1)
    }

    @Test
    fun `given pending PIN verification, when more digits or delete arrive, then entry stays blocked`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val (viewModel, session) = viewModel(fixture)
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        "1234".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }
        runCurrent()

        // WHEN
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(9))
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DeleteTapped)

        // THEN
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.Checking)
        assertThat(viewModel.state.value.enteredDigits).isEqualTo(4)
        gate.complete(Unit)
        runCurrent()
        session.lock()
        "1234".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(fixture.records.getValue(1).failures).isZero()
    }

    @Test
    fun `given partial PIN entry, when the same store is refreshed, then the entered digits remain`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val sites = MutableStateFlow<SiteModel?>(SiteModel().apply { id = 1 })
        val (viewModel, session) = viewModel(fixture, sites)
        "12".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }

        // WHEN
        sites.value = SiteModel().apply {
            id = 1
            name = "Updated store name"
        }
        runCurrent()
        "34".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(viewModel.state.value.enteredDigits).isZero()
    }

    @Test
    fun `given pending unlock, when the same store is refreshed, then the verified role is retained`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val sites = MutableStateFlow<SiteModel?>(SiteModel().apply { id = 1 })
        val (viewModel, session) = viewModel(fixture, sites)
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        "1234".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }
        runCurrent()

        // WHEN
        sites.value = SiteModel().apply {
            id = 1
            name = "Updated store name"
        }
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
        assertThat(viewModel.state.value.isLocked).isFalse()
    }

    @Test
    fun `given pending unlock, when logout clears the selected store, then late success stays blocked`() = runTest {
        // GIVEN
        val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
        val sites = MutableStateFlow<SiteModel?>(SiteModel().apply { id = 1 })
        val (viewModel, session) = viewModel(fixture, sites)
        val gate = CompletableDeferred<Unit>()
        fixture.pinReadGate = gate
        "1234".forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it.digitToInt())) }
        runCurrent()

        // WHEN
        sites.value = null
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isNull()
        assertThat(viewModel.state.value.isLocked).isTrue()
        assertThat(viewModel.state.value.status).isEqualTo(WooPosPINEntryStatus.StorageUnavailable)
    }

    private fun viewModel(
        fixture: WooPosLocalRateLimiterTestFixture,
        sites: MutableStateFlow<SiteModel?> = MutableStateFlow(SiteModel().apply { id = 1 }),
    ): Pair<WooPosLockScreenViewModel, WooPosAccessSession> {
        val selectedSite: SelectedSite = mock {
            on { getOrNull() }.thenAnswer { sites.value }
            on { observe() }.thenReturn(sites)
        }
        val flags: FeatureFlagRepository = mock {
            on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
        }
        val session = WooPosAccessSession(flags, selectedSite, fixture.rateLimiter(), fixture.recovery, fixture.lockPreferences, fixture.cardReaderFacade)
        return WooPosLockScreenViewModel(session, selectedSite, fixture.lockPreferences, fixture.elapsedTime) to session
    }
}
