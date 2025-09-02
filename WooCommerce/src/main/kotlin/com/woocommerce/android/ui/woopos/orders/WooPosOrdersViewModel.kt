package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        refreshOrders()
    }

    fun onOrderSelected(orderId: Long) {
        _state.update { it.copy(selectedOrderId = orderId) }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = ordersDataSource.loadOrders()) {
                is LoadOrdersResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message ?: "Unknown error"
                        )
                    }
                }
                is LoadOrdersResult.Success -> {
                    val list = result.orders
                    _state.update { prev ->
                        prev.copy(
                            isLoading = false,
                            orders = list,
                            selectedOrderId = prev.selectedOrderId?.takeIf { id ->
                                list.any { o -> o.id == id }
                            } ?: list.firstOrNull()?.id
                        )
                    }
                }
            }
        }
    }
}
