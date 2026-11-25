package com.woocommerce.android.ui.woopos.roles.lock

import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosAutoLockTest {
    @get:Rule val coroutineRule = WooPosCoroutineTestRule()
    private val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(site)
        on { observe() }.thenReturn(MutableStateFlow(site))
    }
    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
    }
    private val settings = MutableStateFlow(Result.success(WooPosAutoLockSettings(inactivityMinutes = 1)))

    @Test
    fun `given a one minute interval, when interaction restarts it, then POS locks only after the new interval`() = runTest {
        // GIVEN
        val (viewModel, session) = create()
        viewModel.onForeground()
        advanceTimeBy(40_000)
        fixture.elapsed += 40_000

        // WHEN
        viewModel.onUserInteraction()
        advanceTimeBy(40_000)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
        advanceTimeBy(20_000)
        runCurrent()
        assertThat(session.state.value.isLocked).isTrue()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given background locking off, when the monotonic deadline passes away from POS, then resume locks it`() = runTest {
        // GIVEN
        settings.value = Result.success(WooPosAutoLockSettings(onBackground = false, inactivityMinutes = 1))
        val (viewModel, session) = create()
        viewModel.onForeground()
        advanceTimeBy(30_000)
        fixture.elapsed += 30_000

        // WHEN
        viewModel.onBackground()
        fixture.elapsed += 40_000
        viewModel.onForeground()
        runCurrent()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given Never, when time passes, then inactivity does not lock POS`() = runTest {
        // GIVEN
        settings.value = Result.success(WooPosAutoLockSettings(inactivityMinutes = 0))
        val (viewModel, session) = create()
        viewModel.onForeground()

        // WHEN
        advanceTimeBy(20 * 60_000)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given background locking on, when POS stops, then its operator is cleared immediately`() = runTest {
        // GIVEN
        val (viewModel, session) = create()
        viewModel.onForeground()

        // WHEN
        viewModel.onBackground()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given launch locking off, when an existing POS receives the launcher intent, then its current operator remains`() = runTest {
        // GIVEN
        settings.value = Result.success(WooPosAutoLockSettings(onLaunch = false))
        val (viewModel, session) = create()

        // WHEN
        viewModel.onLauncherEntry()
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given a preference read fails, when its result arrives, then protection stays closed`() = runTest {
        // GIVEN
        val (viewModel, session) = create()

        // WHEN
        settings.value = Result.failure(IllegalStateException())
        runCurrent()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(
            session.state.value.status
        ).isEqualTo(com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus.StorageUnavailable)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given a pending payment, when terminal confirmation arrives, then inactivity starts a full interval`() = runTest {
        // GIVEN
        val payment = MutableStateFlow<WooPosCardReaderFacade.PaymentState>(WooPosCardReaderFacade.PaymentState.Idle)
        whenever(fixture.cardReaderFacade.paymentState).thenReturn(payment)
        whenever(fixture.cardReaderFacade.isPaymentPending).thenAnswer {
            payment.value is WooPosCardReaderFacade.PaymentState.Pending
        }
        val (viewModel, session) = create()
        viewModel.onForeground()
        fixture.elapsed += 40_000
        advanceTimeBy(40_000)
        val owner = WooPosCardReaderFacade.PaymentOwner { false }
        payment.value = WooPosCardReaderFacade.PaymentState.Active(owner)
        runCurrent()

        // WHEN
        fixture.elapsed += 120_000
        advanceTimeBy(120_000)
        payment.value = WooPosCardReaderFacade.PaymentState.Unresolved(owner)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)

        // WHEN
        payment.value = WooPosCardReaderFacade.PaymentState.Idle
        runCurrent()
        advanceTimeBy(59_999)
        runCurrent()

        // THEN
        assertThat(session.state.value.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
        advanceTimeBy(1)
        runCurrent()
        assertThat(session.state.value.isLocked).isTrue()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given no PIN at startup, when background arrives before the PIN read, then initialization still disables protection`() = runTest {
        // GIVEN
        fixture.pins = Result.success(emptyMap())
        fixture.pinReadGate = CompletableDeferred()
        val session = WooPosAccessSession(
            flags,
            selectedSite,
            fixture.rateLimiter(),
            fixture.recovery,
            fixture.lockPreferences,
            fixture.cardReaderFacade
        )
        val viewModel = WooPosLockScreenViewModel(session, selectedSite, fixture.lockPreferences, fixture.elapsedTime)
        runCurrent()

        // WHEN
        viewModel.onBackground()
        fixture.pinReadGate?.complete(Unit)
        runCurrent()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.state.value.activeRole).isNull()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `given first PIN activation finishes in background, when background locking is on, then it cannot restore an operator`() = runTest {
        // GIVEN
        val configured = fixture.pins
        fixture.pins = Result.success(emptyMap())
        val session = WooPosAccessSession(
            flags,
            selectedSite,
            fixture.rateLimiter(),
            fixture.recovery,
            fixture.lockPreferences,
            fixture.cardReaderFacade
        )
        val viewModel = WooPosLockScreenViewModel(session, selectedSite, fixture.lockPreferences, fixture.elapsedTime)
        runCurrent()
        viewModel.onBackground()
        fixture.pins = configured
        fixture.pinReadGate = CompletableDeferred()
        val activation = async { session.unlock("1234") }
        runCurrent()

        // WHEN
        fixture.pinReadGate?.complete(Unit)
        activation.await()
        runCurrent()

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
        viewModel.viewModelScope.cancel()
    }

    private suspend fun create(): Pair<WooPosLockScreenViewModel, WooPosAccessSession> {
        whenever(fixture.lockPreferences.settings(1)).thenReturn(settings)
        val session = WooPosAccessSession(
            flags,
            selectedSite,
            fixture.rateLimiter(),
            fixture.recovery,
            fixture.lockPreferences,
            fixture.cardReaderFacade,
        )
        session.initialize()
        session.unlock("1234")
        val viewModel = WooPosLockScreenViewModel(session, selectedSite, fixture.lockPreferences, fixture.elapsedTime)
        return viewModel to session
    }
}
