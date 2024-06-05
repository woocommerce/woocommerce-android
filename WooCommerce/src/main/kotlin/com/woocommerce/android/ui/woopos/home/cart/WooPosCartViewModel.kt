package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.WooPosBottomUpEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeBottomUpCommunication
import com.woocommerce.android.ui.woopos.home.WooPosHomeUpBottomCommunication
import com.woocommerce.android.ui.woopos.home.WooPosUpBottomEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val bottomUpCommunication: WooPosHomeBottomUpCommunication,
    private val upBottomCommunication: WooPosHomeUpBottomCommunication,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosCartState>(WooPosCartState.Cart)
    val state: StateFlow<WooPosCartState> = _state

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                sendEventUp(WooPosBottomUpEvent.CheckoutClicked)
                _state.value = WooPosCartState.Checkout
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventUp(WooPosBottomUpEvent.BackFromCheckoutToCartClicked)
                _state.value = WooPosCartState.Cart
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            upBottomCommunication.upBottomEventsFlow.collect { event ->
                when (event) {
                    is WooPosUpBottomEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosCartState.Cart
                    }
                }
            }
        }
    }

    private fun sendEventUp(event: WooPosBottomUpEvent) {
        viewModelScope.launch {
            bottomUpCommunication.sendEventUp(event)
        }
    }
}
