package com.woocommerce.android.ui.woopos.home.cart

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OrderDraftCreated
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val repository: WooPosCartRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosCartState(),
        key = "cartViewState"
    )
    val state: StateFlow<WooPosCartState> = _state

    init {
        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                goToTotals()
                createOrderDraft()
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                val currentState = _state.value
                if (currentState.isOrderCreationInProgress) return

                _state.value = currentState.copy(itemsInCart = currentState.itemsInCart - event.item)
            }
        }
    }

    private fun goToTotals() {
        sendEventToParent(ChildToParentEvent.CheckoutClicked)
        _state.value = _state.value.copy(
            isCheckoutButtonVisible = false,
            areItemsRemovable = false,
        )
    }

    private fun createOrderDraft() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(isOrderCreationInProgress = true)

            val result = repository.createOrderWithProducts(
                productIds = currentState.itemsInCart.map {
                    it.productId
                }
            )

            _state.value = _state.value.copy(isOrderCreationInProgress = false)

            result.fold(
                onSuccess = { order ->
                    Log.d("WooPosCartViewModel", "Order created successfully - $order")
                    childrenToParentEventSender.sendToParent(OrderDraftCreated(order.id))
                },
                onFailure = { error ->
                    Log.e("WooPosCartViewModel", "Order creation failed - $error")
                }
            )
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            Log.d("WooPos", "WooPosCartViewModel.listenEventsFromParent() $this")
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = _state.value.copy(
                            isCheckoutButtonVisible = true,
                            areItemsRemovable = true,
                        )
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        if (state.value.isOrderCreationInProgress) return@collect

                        val itemClicked = viewModelScope.async {
                            repository.getProductById(event.productId)?.toCartListItem()!!
                        }

                        val currentState = _state.value
                        _state.value = currentState.copy(
                            itemsInCart = currentState.itemsInCart + itemClicked.await()
                        )
                    }
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        _state.value = WooPosCartState()
                    }
                    else -> Unit
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

private fun Product.toCartListItem(): WooPosCartListItem =
    WooPosCartListItem(
        productId = remoteId,
        title = name
    )
