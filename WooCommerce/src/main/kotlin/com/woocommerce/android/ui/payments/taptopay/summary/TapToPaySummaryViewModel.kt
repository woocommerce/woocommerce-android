package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class TapToPaySummaryViewModel @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData(UiState())
    val viewState: LiveData<UiState> = _viewState

    fun onTryPaymentClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.TAP_TO_PAY_SUMMARY_TRY_PAYMENT_TAPPED)
        launch {
            _viewState.value = UiState(isProgressVisible = true)
            val result = orderCreateEditRepository.createSimplePaymentOrder(TEST_ORDER_AMOUNT)
            result.fold(
                onSuccess = {
                    triggerEvent(StartTryPaymentFlow(it))
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_test_payment_error))
                }
            )
            _viewState.value = UiState(isProgressVisible = false)
        }
    }

    fun onBackClicked() {
        triggerEvent(Event.Exit)
    }

    data class UiState(
        val isProgressVisible: Boolean = false
    )

    data class StartTryPaymentFlow(val order: Order) : Event()

    companion object {
        private val TEST_ORDER_AMOUNT = BigDecimal.valueOf(0.5)
    }
}
