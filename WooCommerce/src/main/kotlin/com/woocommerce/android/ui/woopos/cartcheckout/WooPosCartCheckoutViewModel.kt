package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosCartCheckoutViewModel @Inject constructor() : ViewModel() {
    fun onUIEvent(event: WooPosCartCheckoutUIEvent) {
        when (event) {
            is WooPosCartCheckoutUIEvent.CheckoutClicked -> {
                // Handle checkout clicked event
            }
            is WooPosCartCheckoutUIEvent.BackFromCartToCheckoutClicked -> {
                // Handle back from cart to checkout clicked event
            }
        }
    }
}
