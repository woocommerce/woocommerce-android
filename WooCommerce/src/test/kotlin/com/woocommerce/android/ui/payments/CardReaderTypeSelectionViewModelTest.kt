package com.woocommerce.android.ui.payments

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.EXTERNAL
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardReaderTypeSelectionViewModelTest : BaseUnitTest() {
    private val isTapToPayAvailable: IsTapToPayAvailable = mock {
        on { invoke("US") }.thenReturn(IsTapToPayAvailable.Result.Available)
    }
    private val tracker: CardReaderTracker = mock()

    private var vm = initVm("US")

    @Test
    fun `given tap to pay available, when vm init, then no event emitted`() {
        // GIVEN
        whenever(isTapToPayAvailable("US")).thenReturn(IsTapToPayAvailable.Result.Available)

        // WHEN
        vm = initVm("US")

        // THEN
        assertThat(vm.event.value).isNull()
    }

    @Test
    fun `given tap to pay not available, when vm init, then navigating to hw reader flow`() {
        // GIVEN
        whenever(isTapToPayAvailable("US")).thenReturn(
            IsTapToPayAvailable.Result.NotAvailable.TapToPayDisabled
        )

        // WHEN
        vm = initVm("US")

        // THEN
        assertThat(vm.event.value).isInstanceOf(
            CardReaderTypeSelectionViewModel.NavigateToCardReaderPaymentFlow::class.java
        )
    }

    @Test
    fun `given tap to pay not available, when vm init, then navigating tracks the reason`() {
        // GIVEN
        whenever(isTapToPayAvailable("US")).thenReturn(
            IsTapToPayAvailable.Result.NotAvailable.TapToPayDisabled
        )

        // WHEN
        vm = initVm("US")

        // THEN
        verify(tracker).trackTapToPayNotAvailableReason(
            IsTapToPayAvailable.Result.NotAvailable.TapToPayDisabled
        )
    }

    @Test
    fun `when click on bluetooth reader, then navigate to hw reader flow`() {
        // WHEN
        vm.onUseBluetoothReaderSelected()

        // THEN
        assertThat((vm.event.value as CardReaderTypeSelectionViewModel.NavigateToCardReaderPaymentFlow).cardReaderType)
            .isEqualTo(EXTERNAL)
    }

    @Test
    fun `when click on bluetooth reader, then track bluetooth pick flow`() {
        // WHEN
        vm.onUseBluetoothReaderSelected()

        // THEN
        verify(tracker).trackSelectReaderTypeBluetoothTapped()
    }

    @Test
    fun `when click on tpp, then navigate to tpp flow`() {
        // WHEN
        vm.onUseTapToPaySelected()

        // THEN
        assertThat((vm.event.value as CardReaderTypeSelectionViewModel.NavigateToCardReaderPaymentFlow).cardReaderType)
            .isEqualTo(BUILT_IN)
    }

    @Test
    fun `when click on tpp, then track tpp pick`() {
        // WHEN
        vm.onUseTapToPaySelected()

        // THEN
        verify(tracker).trackSelectReaderTypeBuiltInTapped()
    }

    private fun initVm(country: String) =
        CardReaderTypeSelectionViewModel(
            CardReaderTypeSelectionDialogFragmentArgs(
                mock<CardReaderFlowParam.PaymentOrRefund>(),
                country,
            ).initSavedStateHandle(),
            isTapToPayAvailable,
            tracker
        )
}
