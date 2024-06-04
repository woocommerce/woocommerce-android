package com.woocommerce.android.ui.woopos.cartcheckout.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableLiveData<WooPosCartState>()
    val state: LiveData<WooPosCartState> = _state

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                _state.value = WooPosCartState.Checkout
            }

            is WooPosCartUIEvent.BackFromCheckoytToCartClicked -> {
                _state.value = WooPosCartState.Cart
            }
        }
    }
}
