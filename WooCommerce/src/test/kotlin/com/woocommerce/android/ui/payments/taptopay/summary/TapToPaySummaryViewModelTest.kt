package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TapToPaySummaryViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val viewModel = TapToPaySummaryViewModel(orderCreateEditRepository, savedStateHandle)

    @Test
    fun `give order creation error, when onTryPaymentClicked, then show snackbar`() = testBlocking {
        // GIVEN
        whenever(orderCreateEditRepository.createSimplePaymentOrder(BigDecimal.valueOf(0.5))).thenReturn(
            Result.failure(Exception())
        )

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_test_payment_error)
        )
    }

    @Test
    fun `give order creation success, when onTryPaymentClicked, then navigate to simple payment`() =
        testBlocking {
            // GIVEN
            val order = mock<Order>()
            whenever(orderCreateEditRepository.createSimplePaymentOrder(BigDecimal.valueOf(0.5))).thenReturn(
                Result.success(order)
            )

            // WHEN
            viewModel.onTryPaymentClicked()

            // THEN
            assertThat((viewModel.event.value as TapToPaySummaryViewModel.StartTryPaymentFlow).order).isEqualTo(order)
        }

    @Test
    fun `when onTryPaymentClicked, then progress is shown and then hidden`() = testBlocking {
        // GIVEN
        whenever(orderCreateEditRepository.createSimplePaymentOrder(BigDecimal.valueOf(0.5))).thenReturn(
            Result.failure(Exception())
        )

        val states = viewModel.viewState.captureValues()

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        assertThat(states[0].isProgressVisible).isFalse()
        assertThat(states[1].isProgressVisible).isTrue()
        assertThat(states[2].isProgressVisible).isFalse()
    }

    @Test
    fun `when onBackClicked, then exit emited`() {
        // WHEN
        viewModel.onBackClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(Exit)
    }
}
