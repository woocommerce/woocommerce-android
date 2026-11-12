package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLockScreenViewModelTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
    }
    private val session = WooPosLockSession(featureFlagRepository)
    private val viewModel = WooPosLockScreenViewModel(session)

    @Test
    fun `given locked, when UI submits four digits, then exposed state unlocks on fourth digit`() = runTest {
        // GIVEN
        session.lock()

        // WHEN
        listOf(9, 0, 7).forEach { viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it)) }

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosLockScreenState(isLocked = true, enteredDigits = 3))

        // WHEN
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(8))

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosLockScreenState())
    }

    @Test
    fun `given partial entry, when UI deletes a digit, then exposed entry loses only the last digit`() = runTest {
        // GIVEN
        session.lock()
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(1))
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(2))

        // WHEN
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DeleteTapped)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosLockScreenState(isLocked = true, enteredDigits = 1))
    }

    @Test
    fun `given another ViewModel shares the locked session, when created, then partial entry is retained`() = runTest {
        // GIVEN
        session.lock()
        viewModel.onUIEvent(WooPosLockScreenUIEvent.DigitTapped(1))

        // WHEN
        val anotherViewModel = WooPosLockScreenViewModel(session)

        // THEN
        assertThat(anotherViewModel.state.value)
            .isEqualTo(WooPosLockScreenState(isLocked = true, enteredDigits = 1))
    }
}
