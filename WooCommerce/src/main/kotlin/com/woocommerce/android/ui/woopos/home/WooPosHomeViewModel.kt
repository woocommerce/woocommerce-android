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
    private val _state = MutableStateFlow<WooPosHomeState>(WooPosHomeState.Cart(exitConfirmationDialog = null))
    val state: StateFlow<WooPosHomeState> = _state

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent) {
        when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (val value = _state.value) {
                    WooPosHomeState.Checkout -> {
                        _state.value = WooPosHomeState.Cart(exitConfirmationDialog = null)
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    is WooPosHomeState.Cart -> {
                        _state.value = value.copy(exitConfirmationDialog = WooPosExitConfirmationDialog)
                    }
                }
            }

            WooPosHomeUIEvent.ExitConfirmationDialogDismissed -> {
                _state.value = WooPosHomeState.Cart(exitConfirmationDialog = null)
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
                        _state.value = WooPosHomeState.Cart(exitConfirmationDialog = null)
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.productId)
                        )
                    }
                    is ChildToParentEvent.OrderDraftCreated -> {
                        sendEventToChildren(ParentToChildrenEvent.OrderDraftCreated(event.orderId))
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
