package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor(
    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver,
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        key = "home_state",
        initialValue = WooPosHomeState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.Empty,
            productsInfoDialog = WooPosHomeState.ProductsInfoDialog(isVisible = false),
            exitConfirmationDialog = WooPosHomeState.ExitConfirmationDialog(isVisible = false)
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
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty
                        )
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                    }

                    WooPosHomeState.ScreenPositionState.Checkout.Paid -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty
                        )
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }

                    is WooPosHomeState.ScreenPositionState.Cart -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = WooPosHomeState.ExitConfirmationDialog(isVisible = true)
                        )
                    }
                }
            }

            WooPosHomeUIEvent.ExitConfirmationDialogDismissed -> {
                _state.value = _state.value.copy(
                    exitConfirmationDialog = WooPosHomeState.ExitConfirmationDialog(isVisible = false)
                )
            }

            WooPosHomeUIEvent.DismissProductsInfoDialog -> {
                _state.value = _state.value.copy(
                    productsInfoDialog = WooPosHomeState.ProductsInfoDialog(isVisible = false)
                )
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
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty
                        )
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.productId)
                        )
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.Empty
                        )
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }

                    is ChildToParentEvent.OrderSuccessfullyPaid -> {
                        _state.value = _state.value.copy(
                            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.Paid
                        )
                    }

                    is ChildToParentEvent.CartStatusChanged -> handleCartStatusChanged(event)

                    ChildToParentEvent.ExitPosClicked -> {
                        _state.value = _state.value.copy(
                            exitConfirmationDialog = WooPosHomeState.ExitConfirmationDialog(isVisible = true)
                        )
                    }

                    is ChildToParentEvent.ProductsStatusChanged -> handleProductsStatusChanged(event)

                    ChildToParentEvent.ProductsDialogInfoIconClicked -> {
                        _state.value = _state.value.copy(
                            productsInfoDialog = WooPosHomeState.ProductsInfoDialog(isVisible = true)
                        )
                    }
                }
            }
        }
    }

    private fun handleProductsStatusChanged(event: ChildToParentEvent.ProductsStatusChanged) {
        val screenPosition = _state.value.screenPositionState
        val newScreenPositionState = when (event) {
            ChildToParentEvent.ProductsStatusChanged.FullScreen -> {
                when (screenPosition) {
                    is WooPosHomeState.ScreenPositionState.Cart -> WooPosHomeState.ScreenPositionState.Cart.Hidden
                    is WooPosHomeState.ScreenPositionState.Checkout -> screenPosition
                }
            }
            ChildToParentEvent.ProductsStatusChanged.WithCart -> {
                when (screenPosition) {
                    WooPosHomeState.ScreenPositionState.Cart.Hidden ->
                        WooPosHomeState.ScreenPositionState.Cart.Visible.Empty

                    WooPosHomeState.ScreenPositionState.Cart.Visible.Empty,
                    WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty,
                    WooPosHomeState.ScreenPositionState.Checkout.NotPaid,
                    WooPosHomeState.ScreenPositionState.Checkout.Paid -> screenPosition
                }
            }
        }
        _state.value = _state.value.copy(screenPositionState = newScreenPositionState)
    }

    private fun handleCartStatusChanged(event: ChildToParentEvent.CartStatusChanged) {
        if (_state.value.screenPositionState is WooPosHomeState.ScreenPositionState.Checkout.Paid) {
            return
        }

        val newScreenPositionState = when (event) {
            ChildToParentEvent.CartStatusChanged.Empty -> WooPosHomeState.ScreenPositionState.Cart.Visible.Empty
            ChildToParentEvent.CartStatusChanged.NotEmpty -> WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty
        }
        _state.value = _state.value.copy(screenPositionState = newScreenPositionState)
    }

    private fun sendEventToChildren(event: ParentToChildrenEvent) {
        viewModelScope.launch {
            parentToChildrenEventSender.sendToChildren(event)
        }
    }
}
