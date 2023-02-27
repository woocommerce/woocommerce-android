package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
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
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData(UiState())
    val viewState: LiveData<UiState> = _viewState

    fun onTryPaymentClicked() {
        launch {
            _viewState.value = UiState(isProgressVisible = true)
            val result = orderCreateEditRepository.createSimplePaymentOrder(TEST_ORDER_AMOUNT)
            _viewState.value = UiState(isProgressVisible = false)
            result.fold(
                onSuccess = {},
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.simple_payments_creation_error))
                }
            )
        }
    }

    fun onBackClicked() {
        triggerEvent(Event.Exit)
    }

    data class UiState(
        val isProgressVisible: Boolean = false
    )

    companion object {
        private val TEST_ORDER_AMOUNT = BigDecimal.valueOf(0.5)
    }
}
