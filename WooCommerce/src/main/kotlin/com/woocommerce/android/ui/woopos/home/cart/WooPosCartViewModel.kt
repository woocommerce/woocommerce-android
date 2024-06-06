package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildToParentCommunication
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenCommunication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val bottomUpCommunication: WooPosChildToParentCommunication,
    private val upBottomCommunication: WooPosParentToChildrenCommunication,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosCartState>(WooPosCartState.Cart)
    val state: StateFlow<WooPosCartState> = _state

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                sendEventToParent(ChildToParentEvent.CheckoutClicked)
                _state.value = WooPosCartState.Checkout
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                _state.value = WooPosCartState.Cart
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            upBottomCommunication.parentToChildEventsFlow.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosCartState.Cart
                    }
                }
            }
        }
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            bottomUpCommunication.sendToParent(event)
        }
    }
}
