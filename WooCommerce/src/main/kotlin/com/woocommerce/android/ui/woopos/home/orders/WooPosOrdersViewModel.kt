package com.woocommerce.android.ui.woopos.home.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val repository: WooPosOrdersRepository
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
                            _state.update { 
                                it.copy(
                                    orders = orders,
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
}

data class WooPosOrdersState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)