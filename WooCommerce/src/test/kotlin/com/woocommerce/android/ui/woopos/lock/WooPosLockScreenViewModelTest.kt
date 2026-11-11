package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosPermission
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
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
    fun `given wrong entries, when wait starts, then countdown updates live and Forgot PIN remains available`() = runTest {
        // GIVEN
        val fixture = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
        val site = SiteModel().apply { id = 1 }
        val selectedSite: SelectedSite = mock {
            on { getOrNull() }.thenReturn(site)
            on { observe() }.thenReturn(MutableStateFlow(site))
        }
        val flags: FeatureFlagRepository = mock {
            on { isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK) }.thenReturn(true)
        }
        val viewModel = WooPosLockScreenViewModel(
            WooPosLockSession(flags, selectedSite, fixture.access(), fixture.lockedPOS),
            selectedSite
        )
        repeat(5) {
            repeat(4) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(0)) }
        }
        assertThat(viewModel.state.value.remainingSeconds).isEqualTo(30)

        // WHEN
        val readsBeforeTick = fixture.attemptReads
        fixture.elapsed += 1000
        advanceTimeBy(999)
        runCurrent()
        assertThat(fixture.attemptReads).isEqualTo(readsBeforeTick)
        advanceTimeBy(1)
        runCurrent()
        assertThat(fixture.attemptReads).isEqualTo(readsBeforeTick + 1)
        viewModel.onUIEvent(WooPosLockScreenUIEvent.ForgotPINTapped)

        // THEN
        assertThat(viewModel.state.value.remainingSeconds).isEqualTo(29)
        assertThat(viewModel.state.value.error).isEqualTo(WooPosLockError.RECOVERY_UNAVAILABLE)
        assertThat(viewModel.state.value.isLocked).isTrue()

        // WHEN
        fixture.elapsed += 29_000
        advanceTimeBy(29_000)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value.remainingSeconds).isZero()
        assertThat(viewModel.state.value.canEnterPIN).isTrue()
    }

    @Test
    fun `given shared approval wait while unlocked, when manually locked, then countdown appears without PIN entry`() =
        runTest {
            // GIVEN
            val fixture = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
            val site = SiteModel().apply { id = 1 }
            val selectedSite: SelectedSite = mock {
                on { getOrNull() }.thenReturn(site)
                on { observe() }.thenReturn(MutableStateFlow(site))
            }
            val flags: FeatureFlagRepository = mock {
                on { isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK) }.thenReturn(true)
            }
            val access = fixture.access()
            val session = WooPosLockSession(flags, selectedSite, access, fixture.lockedPOS)
            val viewModel = WooPosLockScreenViewModel(session, selectedSite)
            "5678".forEach { session.enterDigit(it.digitToInt()) }
            assertThat(viewModel.state.value.activeOperator).isEqualTo(fixture.cashier)
            repeat(5) { access.authenticate(1, "0000", WooPosPermission.ISSUE_REFUNDS) }
            val attempts = fixture.records.toMap()

            // WHEN
            session.lock()
            runCurrent()

            // THEN
            assertThat(viewModel.state.value.isLocked).isTrue()
            assertThat(viewModel.state.value.activeOperator).isNull()
            assertThat(viewModel.state.value.remainingSeconds).isEqualTo(30)
            assertThat(viewModel.state.value.canEnterPIN).isFalse()
            repeat(4) { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(0)) }
            assertThat(viewModel.state.value.enteredDigits).isZero()
            assertThat(fixture.records).isEqualTo(attempts)
            viewModel.onUIEvent(WooPosLockScreenUIEvent.ForgotPINTapped)
            assertThat(viewModel.state.value.error).isEqualTo(WooPosLockError.RECOVERY_UNAVAILABLE)
            assertThat(fixture.records).isEqualTo(attempts)

            // WHEN
            fixture.elapsed += 30_000
            advanceTimeBy(30_000)
            runCurrent()

            // THEN
            assertThat(viewModel.state.value.remainingSeconds).isZero()
            assertThat(viewModel.state.value.canEnterPIN).isTrue()
        }
}
