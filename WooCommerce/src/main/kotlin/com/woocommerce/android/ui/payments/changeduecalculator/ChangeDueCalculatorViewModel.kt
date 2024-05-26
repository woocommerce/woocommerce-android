package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ChangeDueCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedStateHandle) {

    val navArgs: ChangeDueCalculatorFragmentArgs by savedStateHandle.navArgs()
    private val orderId: Long = navArgs.orderId

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val amountDue: BigDecimal,
            val change: BigDecimal,
            val amountReceived: BigDecimal
        ) : UiState()
        data object Error : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _navigationEvent = MutableLiveData<Unit>()
    val navigationEvent: LiveData<Unit> = _navigationEvent

    init {
        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        launch {
            val order = orderDetailRepository.getOrderById(orderId)
            order?.let {
                _uiState.value = UiState.Success(amountDue = order.total, change = BigDecimal.ZERO, amountReceived = BigDecimal.ZERO)
            } ?: run {
                _uiState.value = UiState.Error
            }
        }
    }

    fun onBackPressed() {
        _navigationEvent.value = Unit
    }

    fun updateAmountReceived(amount: BigDecimal) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val newChange = amount - currentState.amountDue
            _uiState.value = currentState.copy(amountReceived = amount, change = newChange)
        }
    }
}
