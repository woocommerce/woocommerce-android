package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCartCheckoutViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableLiveData<WooPosCartCheckoutState>(WooPosCartCheckoutState.Cart)
    val state: LiveData<WooPosCartCheckoutState> = _state

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
