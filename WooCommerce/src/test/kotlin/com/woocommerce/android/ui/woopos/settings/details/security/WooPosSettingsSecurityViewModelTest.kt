package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.lock.WooPosLockError
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenUIEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSettingsSecurityViewModelTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()
    private val flags: FeatureFlagRepository = mock()

    @Test
    fun `given Roles off, when mock entry points are called, then no preview opens`() {
        // GIVEN
        val sut = createViewModel(enabled = false)

        // WHEN
        sut.showSetup()
        sut.showLock(WooPosLockScreenState())

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSettingsSecurityState())
    }

    @Test
    fun `given wait, when digits and delete are tapped, then wait remains and recovery stays available`() = runTest {
        // GIVEN
        val sut = createViewModel()
        sut.showLock(WooPosLockScreenState(remainingSeconds = 30))

        // WHEN
        sut.onLockEvent(WooPosLockScreenUIEvent.DigitTapped(1))
        sut.onLockEvent(WooPosLockScreenUIEvent.DeleteTapped)
        sut.onLockEvent(WooPosLockScreenUIEvent.ForgotPINTapped)

        // THEN
        assertThat(sut.state.value.lock).isEqualTo(
            WooPosLockScreenState(isLocked = true, remainingSeconds = 30, error = WooPosLockError.RECOVERY_UNAVAILABLE)
        )
        sut.close()
    }

    @Test
    fun `given countdown preview, when time passes, then countdown reaches zero and input enables`() = runTest {
        // GIVEN
        val sut = createViewModel()
        sut.showLock(WooPosLockScreenState(remainingSeconds = 2))
        runCurrent()

        // WHEN
        advanceTimeBy(2_000)
        runCurrent()

        // THEN
        assertThat(sut.state.value.lock?.remainingSeconds).isZero()
        assertThat(sut.state.value.lock?.canEnterPIN).isTrue()
    }

    @Test
    fun `given entry, when four digits are pressed, then mock remains locked and checking`() {
        // GIVEN
        val sut = createViewModel()
        sut.showLock(WooPosLockScreenState())

        // WHEN
        repeat(4) { sut.onLockEvent(WooPosLockScreenUIEvent.DigitTapped(it)) }

        // THEN
        assertThat(sut.state.value.lock).isEqualTo(
            WooPosLockScreenState(isLocked = true, enteredDigits = 4, isChecking = true)
        )
    }

    @Test
    fun `given storage unavailable, when input is attempted, then no digit is entered`() {
        // GIVEN
        val sut = createViewModel()
        sut.showLock(WooPosLockScreenState(error = WooPosLockError.STORAGE))

        // WHEN
        sut.onLockEvent(WooPosLockScreenUIEvent.DigitTapped(1))

        // THEN
        assertThat(sut.state.value.lock?.enteredDigits).isZero()
    }

    @Test
    fun `given entered setup digits, when closed and reopened, then entry resets`() {
        // GIVEN
        val sut = createViewModel()
        sut.showSetup(WooPosPinSetupState(enteredDigits = 2))

        // WHEN
        sut.close()
        sut.showSetup()

        // THEN
        assertThat(sut.setupState.value).isEqualTo(WooPosPinSetupState())
    }

    private fun createViewModel(enabled: Boolean = true): WooPosSettingsSecurityViewModel {
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(enabled)
        return WooPosSettingsSecurityViewModel(WooPosMockPinSetup(), flags)
    }
}
