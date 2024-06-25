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
    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver,
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosHomeState>(WooPosHomeState.Cart)
    val state: StateFlow<WooPosHomeState> = _state

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent): Boolean {
        return when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value) {
                    WooPosHomeState.Checkout -> {
                        _state.value = WooPosHomeState.Cart
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                        true
                    }

                    is WooPosHomeState.Cart -> {
                        false
                    }
                }
            }
        }
    }

    private fun listenBottomEvents() {
        viewModelScope.launch {
            childrenToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = WooPosHomeState.Checkout
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosHomeState.Cart
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.productId)
                        )
                    }
                    is ChildToParentEvent.OrderDraftCreated -> {
                        sendEventToChildren(ParentToChildrenEvent.OrderDraftCreated(event.orderId))
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = WooPosHomeState.Cart
                    }
                    is ChildToParentEvent.OrderSuccessfullyPaid -> {
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }
                }
            }
        }
    }

    private fun sendEventToChildren(event: ParentToChildrenEvent) {
        viewModelScope.launch {
            parentToChildrenEventSender.sendToChildren(event)
        }
    }
}
