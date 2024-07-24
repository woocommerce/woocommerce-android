package com.woocommerce.android.ui.woopos.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
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
    private val _state = MutableStateFlow<WooPosHomeState>(WooPosHomeState.Cart.Empty)
    val state: StateFlow<WooPosHomeState> = _state

    init {
        listenBottomEvents()
    }

    fun onUIEvent(event: WooPosHomeUIEvent): Boolean {
        return when (event) {
            WooPosHomeUIEvent.SystemBackClicked -> {
                when (_state.value) {
                    WooPosHomeState.Checkout.NotPaid -> {
                        _state.value = WooPosHomeState.Cart.NotEmpty
                        sendEventToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
                        true
                    }

                    WooPosHomeState.Checkout.Paid -> {
                        _state.value = WooPosHomeState.Cart.Empty
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                        true
                    }

                    is WooPosHomeState.Cart -> {
                        false
                    }

                    is WooPosHomeState.ProductsInfoDialog -> {
                        false
                    }
                }
            }

            WooPosHomeUIEvent.DismissProductsInfoDialog -> {
                _state.value = (_state.value as? WooPosHomeState.ProductsInfoDialog)?.copy(
                    shouldDisplayDialog = false
                )!!
                true
            }
        }
    }

    private fun listenBottomEvents() {
        viewModelScope.launch {
            childrenToParentEventReceiver.events.collect { event ->
                when (event) {
                    is ChildToParentEvent.CheckoutClicked -> {
                        _state.value = WooPosHomeState.Checkout.NotPaid
                        sendEventToChildren(ParentToChildrenEvent.CheckoutClicked(event.productIds))
                    }

                    is ChildToParentEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosHomeState.Cart.NotEmpty
                    }

                    is ChildToParentEvent.ItemClickedInProductSelector -> {
                        sendEventToChildren(
                            ParentToChildrenEvent.ItemClickedInProductSelector(event.productId)
                        )
                    }

                    is ChildToParentEvent.NewTransactionClicked -> {
                        _state.value = WooPosHomeState.Cart.Empty
                        sendEventToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
                    }

                    is ChildToParentEvent.OrderSuccessfullyPaid -> {
                        _state.value = WooPosHomeState.Checkout.Paid
                    }

                    is ChildToParentEvent.CartStatusChanged -> {
                        if (_state.value is WooPosHomeState.Checkout.Paid) return@collect

                        when (event) {
                            ChildToParentEvent.CartStatusChanged.Empty -> {
                                _state.value = WooPosHomeState.Cart.Empty
                            }

                            ChildToParentEvent.CartStatusChanged.NotEmpty -> {
                                _state.value = WooPosHomeState.Cart.NotEmpty
                            }
                        }
                    }

                    ChildToParentEvent.ProductsDialogInfoIconClicked -> {
                        _state.value = WooPosHomeState.ProductsInfoDialog(
                            shouldDisplayDialog = true,
                            header = R.string.woopos_dialog_products_info_heading,
                            primaryMessage = R.string.woopos_dialog_products_info_primary_message,
                            secondaryMessage = R.string.woopos_dialog_products_info_secondary_message,
                            secondaryMessageActionLabel =
                            R.string.woopos_dialog_products_info_secondary_message_action_label,
                            primaryButton = WooPosHomeState.ProductsInfoDialog.PrimaryButton(
                                label = R.string.woopos_dialog_products_info_button_label,
                            ),
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
