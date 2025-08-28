package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

            val result = ordersDataSource.loadOrders()

            if (result.isError || result.model == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.error?.message ?: "Unknown error"
                    )
                }
                return@launch
            }

            result.model?.let { list ->
                _state.update { prev ->
                    prev.copy(
                        isLoading = false,
                        orders = list,
                        selectedOrderId = prev.selectedOrderId?.takeIf { id -> list.any { o -> o.id == id } }
                            ?: list.firstOrNull()?.id
                    )
                }
            }
        }
    }
}
