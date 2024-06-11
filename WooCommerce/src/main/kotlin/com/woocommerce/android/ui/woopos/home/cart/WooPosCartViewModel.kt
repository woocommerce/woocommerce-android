package com.woocommerce.android.ui.woopos.home.cart

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val productStore: WCProductStore,
    private val site: SelectedSite,
    private val repository: WooPosCartRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow<WooPosCartState>(
        scope = viewModelScope,
        initialValue = WooPosCartState.Cart(itemsInCart = emptyList()),
        key = "cartViewState"
    )
    val state: StateFlow<WooPosCartState> = _state

    init {
        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                sendEventToParent(ChildToParentEvent.CheckoutClicked)
                createOrderDraft()
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                _state.value = WooPosCartState.Cart(
                    itemsInCart = state.value.itemsInCart,
                    isLoading = state.value.isLoading
                )
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                if (state.value.isLoading) return

                _state.update { state ->
                    val itemsInCart = state.itemsInCart - event.item
                    when (state) {
                        is WooPosCartState.Cart -> state.copy(
                            itemsInCart = itemsInCart,
                            isLoading = false,
                        )

                        is WooPosCartState.Checkout -> state.copy(
                            itemsInCart = itemsInCart,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun createOrderDraft() {
        viewModelScope.launch {
            val itemsInCart = _state.value.itemsInCart
            _state.value = WooPosCartState.Checkout(
                state.value.itemsInCart,
                isLoading = true,
            )

            val productIds = itemsInCart.map { it.productId }
            val result = repository.createOrderWithProducts(productIds = productIds)

            _state.value = WooPosCartState.Checkout(
                itemsInCart,
                isLoading = false
            )

            result.fold(
                onSuccess = { order ->
                    Log.d("WooPosCartViewModel", "Order created successfully - $order")
                },
                onFailure = { error ->
                    Log.e("WooPosCartViewModel", "Order creation failed - $error")
                }
            )
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = WooPosCartState.Cart(
                            itemsInCart = state.value.itemsInCart,
                            isLoading = state.value.isLoading
                        )
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        if (state.value.isLoading) return@collect

                        val site = requireNotNull(site.getOrNull())
                        val itemClicked = viewModelScope.async {
                            productStore.getProductByRemoteId(site, event.productId)!!.toCartListItem()
                        }
                        val itemsInCart = _state.value.itemsInCart + itemClicked.await()
                        _state.value = when (val state = _state.value) {
                            is WooPosCartState.Cart -> state.copy(itemsInCart = itemsInCart)
                            is WooPosCartState.Checkout -> state.copy(itemsInCart = itemsInCart)
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

private fun WCProductModel.toCartListItem(): WooPosCartListItem =
    WooPosCartListItem(
        productId = id.toLong(),
        title = name
    )
