package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChangeDueCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository
) : ViewModel() {

    private val orderId: Long = savedStateHandle.get<Long>("orderId")
        ?: throw IllegalArgumentException("OrderId is required")

    sealed class UiState {
        object Loading : UiState()
        data class Success(val amountDue: BigDecimal, val change: BigDecimal) : UiState()
        object Error : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadOrderDetails()
    }

    fun loadOrderDetails() {
        viewModelScope.launch {
            try {
                val order = orderDetailRepository.getOrderById(orderId)
                if (order != null) {
                    _uiState.value = UiState.Success(amountDue = order.total, 0.00.toBigDecimal())
                } else {
                    _uiState.value = UiState.Error
                }
            } catch (e: IOException) {
                WooLog.e(
                    tag = WooLog.T.ORDERS,
                    message = "Error loading order details"
                )
                _uiState.value = UiState.Error
            }
        }
    }
}
