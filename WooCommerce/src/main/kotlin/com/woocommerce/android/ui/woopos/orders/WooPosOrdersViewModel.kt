package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(WooPosOrdersState())
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    fun onOrderSelected(orderId: Long) {
        _state.update { it.copy(selectedOrderId = orderId) }
    }
}
