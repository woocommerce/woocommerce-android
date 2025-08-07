package com.woocommerce.android.ui.woopos.home.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val repository: WooPosOrdersRepository,
    val currencyFormatter: CurrencyFormatter
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosOrdersState())
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.fetchPosOrders()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = { orders ->
                            val displayModels = orders.map { order ->
                                OrderDisplayModel(
                                    order = order,
                                    formattedTotal = currencyFormatter.formatCurrency(order.total, order.currency)
                                )
                            }
                            _state.update {
                                it.copy(
                                    orders = displayModels,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message
                                )
                            }
                        }
                    )
                }
        }
    }

    fun processRefund(
        order: Order,
        amount: BigDecimal,
        reason: String,
        method: RefundMethod,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.processRefund(
                orderId = order.id,
                amount = amount,
                reason = reason,
                method = method
            )

            result.fold(
                onSuccess = {
                    onResult(true)
                    loadOrders()
                },
                onFailure = {
                    onResult(false)
                }
            )
        }
    }

    suspend fun getOrderRefunds(order: Order): Result<List<Refund>> {
        return repository.fetchOrderRefunds(order.id)
    }
}

data class WooPosOrdersState(
    val orders: List<OrderDisplayModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OrderDisplayModel(
    val order: Order,
    val formattedTotal: String
)

enum class RefundMethod {
    CASH,
    CARD
}
