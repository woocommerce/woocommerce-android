package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.CHECKOUT
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EDITABLE
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EMPTY
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val getProductById: WooPosGetProductById,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosCartState(),
        key = "cartViewState"
    )

    val state: LiveData<WooPosCartState> = _state
        .scan(_state.value) { previousState, newState ->
            updateParentCartStatusIfCartChanged(previousState, newState)
            newState
        }
        .asLiveData()
        .map { updateCartStatusDependingOnItems(it) }
        .map { updateToolbarState(it) }
        .map { updateStateDependingOnCartStatus(it) }

    init {
        listenEventsFromParent()
    }

    @Suppress("ReturnCount")
    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                goToTotals()
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                val currentState = _state.value
                _state.value = if (currentState.body.amountOfItems == 1) {
                    currentState.copy(body = WooPosCartState.Body.Empty)
                } else {
                    currentState.copy(
                        body = (currentState.body as WooPosCartState.Body.WithItems)
                            .copy(itemsInCart = currentState.body.itemsInCart - event.item)
                    )
                }
            }

            WooPosCartUIEvent.BackClicked -> {
                val currentState = _state.value
                if (currentState.cartStatus == EDITABLE) {
                    return
                }
                _state.value = currentState.copy(cartStatus = EDITABLE)

                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
            }

            WooPosCartUIEvent.ClearAllClicked -> clearCart()
        }
    }

    private fun clearCart() {
        val currentState = _state.value
        _state.value = currentState.copy(
            body = WooPosCartState.Body.Empty
        )
    }

    private fun goToTotals() {
        val productIds = (_state.value.body as WooPosCartState.Body.WithItems).itemsInCart.map { it.id.productId }
        sendEventToParent(ChildToParentEvent.CheckoutClicked(productIds))
        _state.value = _state.value.copy(cartStatus = CHECKOUT)
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = _state.value.copy(cartStatus = EDITABLE)
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        val itemClicked = async {
                            val product = getProductById(event.productId)!!
                            val itemNumber = when (val currentState = _state.value.body) {
                                is WooPosCartState.Body.Empty -> 1
                                is WooPosCartState.Body.WithItems ->
                                    (currentState.itemsInCart.maxOfOrNull { it.id.itemNumber } ?: 0) + 1
                            }
                            product.toCartListItem(itemNumber)
                        }
                        _state.value = when (val currentState = _state.value.body) {
                            is WooPosCartState.Body.Empty -> _state.value.copy(
                                body = WooPosCartState.Body.WithItems(listOf(itemClicked.await()))
                            )

                            is WooPosCartState.Body.WithItems -> _state.value.copy(
                                body = currentState.copy(
                                    itemsInCart = currentState.itemsInCart + itemClicked.await()
                                )
                            )
                        }
                    }

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        _state.value = WooPosCartState()
                    }

                    is ParentToChildrenEvent.CheckoutClicked -> {
                        // Do nothing
                    }

                    ParentToChildrenEvent.ProductsLoading -> {
                        clearCart()
                    }
                }
            }
        }
    }

    private fun updateToolbarState(newState: WooPosCartState): WooPosCartState {
        val itemsCount = resourceProvider.getQuantityString(
            newState.body.amountOfItems,
            default = R.string.woopos_items_in_cart_multiple,
            zero = R.string.woopos_items_in_cart_multiple,
            one = R.string.woopos_items_in_cart,
        )
        val newToolbar = when (newState.cartStatus) {
            EDITABLE -> {
                WooPosCartState.Toolbar(
                    icon = null,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = newState.body is WooPosCartState.Body.WithItems
                )
            }

            CHECKOUT -> {
                WooPosCartState.Toolbar(
                    icon = R.drawable.ic_back_24dp,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = false
                )
            }

            EMPTY -> {
                WooPosCartState.Toolbar(
                    icon = null,
                    itemsCount = null,
                    isClearAllButtonVisible = false
                )
            }
        }
        return newState.copy(toolbar = newToolbar)
    }

    private fun updateStateDependingOnCartStatus(newState: WooPosCartState) =
        when (newState.cartStatus) {
            EDITABLE -> {
                newState.copy(
                    areItemsRemovable = true,
                    isCheckoutButtonVisible = newState.body is WooPosCartState.Body.WithItems
                )
            }

            CHECKOUT, EMPTY -> {
                newState.copy(
                    areItemsRemovable = false,
                    isCheckoutButtonVisible = false,
                )
            }
        }

    private fun updateParentCartStatusIfCartChanged(previousState: WooPosCartState, newState: WooPosCartState) {
        if (previousState.body.amountOfItems == newState.body.amountOfItems) return
        when (newState.body) {
            is WooPosCartState.Body.Empty -> {
                sendEventToParent(ChildToParentEvent.CartStatusChanged.Empty)
            }

            is WooPosCartState.Body.WithItems -> {
                sendEventToParent(ChildToParentEvent.CartStatusChanged.NotEmpty)
            }
        }
    }

    private fun updateCartStatusDependingOnItems(newState: WooPosCartState): WooPosCartState =
        when (newState.body) {
            is WooPosCartState.Body.Empty -> newState.copy(cartStatus = EMPTY)
            is WooPosCartState.Body.WithItems -> newState
        }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(event)
        }
    }

    private suspend fun Product.toCartListItem(itemNumber: Int): WooPosCartState.Body.WithItems.Item =
        WooPosCartState.Body.WithItems.Item(
            id = WooPosCartState.Body.WithItems.Item.Id(productId = remoteId, itemNumber = itemNumber),
            name = name,
            price = formatPrice(price),
            imageUrl = firstImageUrl
        )
}
