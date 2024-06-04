package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosCartCheckoutViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<WooPosCartCheckoutState>(WooPosCartCheckoutState.Cart)
    val state: StateFlow<WooPosCartCheckoutState> = _state

    fun onUIEvent(event: WooPosCartCheckoutUIEvent) {
        when (event) {
            is WooPosCartCheckoutUIEvent.CheckoutClicked -> {
                _state.value = WooPosCartCheckoutState.Checkout
            }

            is WooPosCartCheckoutUIEvent.BackFromCheckoytToCartClicked -> {
                _state.value = WooPosCartCheckoutState.Cart
            }
        }
    }
}
