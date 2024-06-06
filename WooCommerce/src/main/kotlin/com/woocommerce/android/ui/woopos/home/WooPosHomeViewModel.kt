package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor(
    private val bottomUpCommunication: WooPosChildToParentCommunication,
    private val upBottomCommunication: WooPosParentToChildrenCommunication,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosHomeState>(WooPosHomeState.Cart)
    val state: StateFlow<WooPosHomeState> = _state

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent) {
        when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value) {
                    WooPosHomeState.Checkout -> {
                        _state.value = WooPosHomeState.Cart
                        sendEventDown(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    WooPosHomeState.Cart -> {
                        TODO("exit from the POS is not implemented yet")
                    }
                }
            }
        }
    }

    private fun listenBottomEvents() {
        viewModelScope.launch {
            bottomUpCommunication.childToParentEventsFlow.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = WooPosHomeState.Checkout
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosHomeState.Cart
                    }
                }
            }
        }
    }

    private fun sendEventDown(event: ParentToChildrenEvent) {
        viewModelScope.launch {
            upBottomCommunication.sendToChildren(event)
        }
    }
}
