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
                _state.value = currentState.copy(
                    body = currentState.body.copy(itemsInCart = currentState.body.itemsInCart - event.item)
                )
            }

            WooPosCartUIEvent.BackClicked -> {
                val currentState = _state.value
                if (currentState.cartStatus == EDITABLE) {
                    return
                }
                _state.value = currentState.copy(cartStatus = EDITABLE)

                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
            }

            WooPosCartUIEvent.ClearAllClicked -> {
                val currentState = _state.value
                _state.value = currentState.copy(
                    body = currentState.body.copy(itemsInCart = emptyList())
                )
            }
        }
    }

    private fun goToTotals() {
        val productIds = _state.value.body.itemsInCart.map { it.id.productId }
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
                        val itemClicked = viewModelScope.async {
                            val product = getProductById(event.productId)!!
                            val itemNumber = (_state.value.body.itemsInCart.maxOfOrNull { it.id.itemNumber } ?: 0) + 1
                            product.toCartListItem(itemNumber)
                        }

                        val currentState = _state.value
                        _state.value = currentState.copy(
                            body = currentState.body.copy(
                                itemsInCart = currentState.body.itemsInCart + itemClicked.await()
                            ),
                        )
                    }

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        _state.value = WooPosCartState()
                    }

                    is ParentToChildrenEvent.CheckoutClicked -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun updateToolbarState(newState: WooPosCartState): WooPosCartState {
        val itemsCount = if (newState.body.itemsInCart.isNotEmpty()) {
            resourceProvider.getString(
                R.string.woopos_items_in_cart,
                newState.body.itemsInCart.size
            )
        } else {
            ""
        }
        val newToolbar = when (newState.cartStatus) {
            EDITABLE -> {
                WooPosCartToolbar(
                    icon = null,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = newState.body.itemsInCart.isNotEmpty()
                )
            }

            CHECKOUT -> {
                WooPosCartToolbar(
                    icon = R.drawable.ic_back_24dp,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = false
                )
            }

            EMPTY -> TODO()
        }
        return newState.copy(toolbar = newToolbar)
    }

    private fun updateStateDependingOnCartStatus(newState: WooPosCartState) =
        when (newState.cartStatus) {
            EDITABLE -> {
                newState.copy(
                    areItemsRemovable = true,
                    isCheckoutButtonVisible = newState.body.itemsInCart.isNotEmpty(),
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
        if (previousState.body.itemsInCart.size == newState.body.itemsInCart.size) return
        if (newState.body.itemsInCart.isNotEmpty()) {
            sendEventToParent(ChildToParentEvent.CartStatusChanged.NotEmpty)
        } else {
            sendEventToParent(ChildToParentEvent.CartStatusChanged.Empty)
        }
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(event)
        }
    }

    private suspend fun Product.toCartListItem(itemNumber: Int): WooPosCartListItem =
        WooPosCartListItem(
            id = WooPosCartListItem.Id(productId = remoteId, itemNumber = itemNumber),
            name = name,
            price = formatPrice(price),
            imageUrl = firstImageUrl
        )
}
