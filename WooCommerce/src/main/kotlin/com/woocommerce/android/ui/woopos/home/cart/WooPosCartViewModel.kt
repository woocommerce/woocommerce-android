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
                sendEventToParent(ChildToParentEvent.CheckoutClicked)
                createOrderDraft()
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                val currentState = state.value
                if (currentState.isOrderCreationInProgress) return

                _state.value = currentState.copy(itemsInCart = currentState.itemsInCart - event.item)
            }
        }
    }

    private fun createOrderDraft() {
        viewModelScope.launch {
            val currentState = state.value
            _state.value = currentState.copy(isOrderCreationInProgress = true)

            val result = repository.createOrderWithProducts(
                productIds = currentState.itemsInCart.map { it.productId }
            )

            _state.value = currentState.copy(isOrderCreationInProgress = false)

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
                        _state.value = _state.value.copy(isCheckoutButtonVisible = true)
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> {
                        if (state.value.isOrderCreationInProgress) return@collect

                        val siteModel = site.get()
                        val itemClicked = viewModelScope.async {
                            productStore.getProductByRemoteId(siteModel, event.productId)!!.toCartListItem()
                        }

                        val currentState = _state.value
                        _state.value = currentState.copy(
                            itemsInCart = currentState.itemsInCart + itemClicked.await()
                        )
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
