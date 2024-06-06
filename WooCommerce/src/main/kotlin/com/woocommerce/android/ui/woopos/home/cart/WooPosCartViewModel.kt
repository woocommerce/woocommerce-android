package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val _state = MutableStateFlow<WooPosCartState>(WooPosCartState.Cart(emptyList()))
    val state: StateFlow<WooPosCartState> = _state

    private val itemsInCart: MutableStateFlow<List<WooPosProductsListItem>> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "itemsInCart"
    )

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                sendEventToParent(ChildToParentEvent.CheckoutClicked)
                _state.value = WooPosCartState.Checkout(itemsInCart.value)
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                _state.value = WooPosCartState.Cart(itemsInCart.value)
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosCartState.Cart(itemsInCart.value)
                    }

                    is ParentToChildrenEvent.ProductSelectionChangedInProductSelector -> {
                        itemsInCart.update { event.selectedItems }
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
