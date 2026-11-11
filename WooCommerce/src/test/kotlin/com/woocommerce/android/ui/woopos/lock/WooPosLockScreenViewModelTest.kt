package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
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
        fixture.elapsed += 1000
        advanceTimeBy(1000)
        runCurrent()
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
}
