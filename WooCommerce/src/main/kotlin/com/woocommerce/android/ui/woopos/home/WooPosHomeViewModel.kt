package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<WooPosHomeState>(WooPosHomeState.Cart)
    val state: StateFlow<WooPosHomeState> = _state

    fun onUIEvent(event: WooPosHomeUIEvent) {
        when (event) {
            is WooPosHomeUIEvent.CheckoutClicked -> {
                _state.value = WooPosHomeState.Checkout
            }

            is WooPosHomeUIEvent.BackFromCheckoutToCartClicked -> {
                _state.value = WooPosHomeState.Cart
            }

            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value) {
                    WooPosHomeState.Checkout -> {
                        _state.value = WooPosHomeState.Cart
                    }

                    WooPosHomeState.Cart -> {
                        TODO("exit from the POS is not implemented yet")
                    }
                }
            }
        }
    }
}
