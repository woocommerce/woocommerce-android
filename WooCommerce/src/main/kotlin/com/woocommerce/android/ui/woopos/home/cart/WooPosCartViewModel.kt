package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow<WooPosCartState>(
        scope = viewModelScope,
        initialValue = WooPosCartState.Cart(emptyList()),
        key = "cartViewState"
    )
    val state: StateFlow<WooPosCartState> = _state

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                sendEventToParent(ChildToParentEvent.CheckoutClicked)
                _state.update { state ->
                    WooPosCartState.Checkout(state.itemsInCart)
                }
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                _state.update { state ->
                    WooPosCartState.Cart(state.itemsInCart)
                }
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                sendEventToParent(ChildToParentEvent.ItemRemovedFromCart(event.item))
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.update { state ->
                            WooPosCartState.Cart(state.itemsInCart)
                        }
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        _state.update { state ->
                            val itemsInCart = state.itemsInCart + event.selectedItem
                            when (state) {
                                is WooPosCartState.Cart -> state.copy(itemsInCart = itemsInCart)
                                is WooPosCartState.Checkout -> state.copy(itemsInCart = itemsInCart)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(event)
        }
    }
}
