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
    private val _state = MutableStateFlow(
        WooPosHomeState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Empty,
            exitConfirmationDialog = null
        )
    )
    val state: StateFlow<WooPosHomeState> = _state

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent) {
        return when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value.screenPositionState) {
                    WooPosHomeState.ScreenPositionState.Checkout.NotPaid -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.NotEmpty
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    WooPosHomeState.ScreenPositionState.Checkout.Paid -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Empty
                        )
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }

                    is WooPosHomeState.ScreenPositionState.Cart -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = WooPosExitConfirmationDialog
                        )
                    }
                }
            }

            WooPosHomeUIEvent.ExitConfirmationDialogDismissed -> {
                _state.value = _state.value.copy(exitConfirmationDialog = null)
            }
        }
    }

    private fun listenBottomEvents() {
        viewModelScope.launch {
            childrenToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.NotPaid
                        )
                        sendEventToChildren(ParentToChildrenEvent.CheckoutClicked(event.productIds))
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.NotEmpty
                        )
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.productId)
                        )
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Empty
                        )
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }

                    is ChildToParentEvent.OrderSuccessfullyPaid -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.Paid
                        )
                    }

                    is ChildToParentEvent.CartStatusChanged -> {
                        if (_state.value.screenPositionState is WooPosHomeState.ScreenPositionState.Checkout.Paid) {
                            return@collect
                        }

                        when (event) {
                            ChildToParentEvent.CartStatusChanged.Empty -> {
                                _state.value = _state.value.copy(
                                    screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Empty
                                )
                            }

                            ChildToParentEvent.CartStatusChanged.NotEmpty -> {
                                _state.value = _state.value.copy(
                                    screenPositionState = WooPosHomeState.ScreenPositionState.Cart.NotEmpty
                                )
                            }
                        }
                    }

                    ChildToParentEvent.ExitPosClicked -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = WooPosExitConfirmationDialog
                        )
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
