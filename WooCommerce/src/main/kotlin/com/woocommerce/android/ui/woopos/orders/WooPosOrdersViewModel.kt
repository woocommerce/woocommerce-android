package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WooPosOrdersDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosOrdersState())
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    fun onOrderSelected(orderId: Long) {
        _state.update { it.copy(selectedOrderId = orderId) }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            ordersDataSource.loadOrders().collect { res ->
                when (res) {
                    is WooPosOrdersDataSource.OrdersResult.Cached -> {
                        // Show cache immediately, keep loading=true while remote is in flight
                        val list = res.orders
                        _state.update { prev ->
                            prev.copy(
                                orders = list,
                                // preserve selection if still present; otherwise pick first
                                selectedOrderId = prev.selectedOrderId?.takeIf { id -> list.any { o -> o.id == id } }
                                    ?: list.firstOrNull()?.id
                            )
                        }
                    }
                    is WooPosOrdersDataSource.OrdersResult.Remote -> {
                        res.ordersResult.fold(
                            onSuccess = { list ->
                                _state.update { prev ->
                                    prev.copy(
                                        isLoading = false,
                                        error = null,
                                        orders = list,
                                        selectedOrderId = prev.selectedOrderId
                                            ?.takeIf { id -> list.any { o -> o.id == id } }
                                            ?: list.firstOrNull()?.id
                                    )
                                }
                            },
                            onFailure = { err ->
                                _state.update { it.copy(isLoading = false, error = err.message ?: "Unknown error") }
                            }
                        )
                    }
                }
            }
        }
    }
}
