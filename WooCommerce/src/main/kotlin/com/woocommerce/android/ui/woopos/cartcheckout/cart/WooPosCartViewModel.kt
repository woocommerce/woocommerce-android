package com.woocommerce.android.ui.woopos.cartcheckout.cart

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<WooPosCartState>(WooPosCartState.Cart)
    val state: StateFlow<WooPosCartState> = _state

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                _state.value = WooPosCartState.Checkout
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                _state.value = WooPosCartState.Cart
            }
        }
    }
}
