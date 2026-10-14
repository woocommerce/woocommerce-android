package com.woocommerce.android.ui.woopos.settings.details.payments

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosSettingsPaymentsViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val preferencesRepository: WooPosPreferencesRepository = mock()

    @Test
    fun `given preference not loaded yet, when viewmodel is initialized, then state is Loading`() = runTest {
        // GIVEN
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(emptyFlow())

        // WHEN
        val sut = createViewModel()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSettingsPaymentsState.Loading)
    }

    @Test
    fun `given sound enabled, when viewmodel is initialized, then toggle is on`() = runTest {
        // GIVEN
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(flowOf(true))

        // WHEN
        val sut = createViewModel()

        // THEN
        assertThat(sut.state.value)
            .isEqualTo(WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = true))
    }

    @Test
    fun `given sound disabled, when viewmodel is initialized, then toggle is off`() = runTest {
        // GIVEN
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(flowOf(false))

        // WHEN
        val sut = createViewModel()

        // THEN
        assertThat(sut.state.value)
            .isEqualTo(WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = false))
    }

    @Test
    fun `given toggle on, when preference changes to disabled, then toggle is off`() = runTest {
        // GIVEN
        val preference = MutableStateFlow(true)
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(preference)
        val sut = createViewModel()

        // WHEN
        preference.value = false

        // THEN
        assertThat(sut.state.value)
            .isEqualTo(WooPosSettingsPaymentsState.Content(isPaymentSuccessSoundEnabled = false))
    }

    @Test
    fun `when sound toggled off, then preference is saved as disabled`() = runTest {
        // GIVEN
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(flowOf(true))
        val sut = createViewModel()

        // WHEN
        sut.onUIEvent(WooPosSettingsPaymentsUIEvent.PaymentSuccessSoundToggled(isEnabled = false))

        // THEN
        verify(preferencesRepository).setPaymentSuccessSoundEnabled(false)
    }

    @Test
    fun `when sound toggled on, then preference is saved as enabled`() = runTest {
        // GIVEN
        whenever(preferencesRepository.isPaymentSuccessSoundEnabled).thenReturn(flowOf(false))
        val sut = createViewModel()

        // WHEN
        sut.onUIEvent(WooPosSettingsPaymentsUIEvent.PaymentSuccessSoundToggled(isEnabled = true))

        // THEN
        verify(preferencesRepository).setPaymentSuccessSoundEnabled(true)
    }

    private fun createViewModel() = WooPosSettingsPaymentsViewModel(
        preferencesRepository = preferencesRepository,
    )
}
