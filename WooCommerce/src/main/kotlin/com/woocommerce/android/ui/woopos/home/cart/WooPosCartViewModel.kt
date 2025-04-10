package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.CHECKOUT
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EDITABLE
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EMPTY
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.variations.getNameForPOS
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CheckoutTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ClearCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.InteractionWithCustomerStarted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemRemovedFromCart
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val getProductById: WooPosGetProductById,
    private val getVariationsById: WooPosGetVariationById,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsTrackingDataKeeper: WooPosAnalyticsTrackingDataKeeper,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosCartState(),
        key = "cartViewState"
    )

    val state: LiveData<WooPosCartState> = _state
        .asLiveData()
        .map { updateCartStatusDependingOnItems(it).also { newState -> updateAnalyticsData(newState) } }
        .map { updateToolbarState(it) }
        .map { updateStateDependingOnCartStatus(it) }

    private val itemNumberProvider = AtomicInteger(getInitialValueOrHighestUsedItemNumberAfterProcessDeath())

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
                viewModelScope.launch {
                    analyticsTracker.track(ItemRemovedFromCart)
                }
            }

            WooPosCartUIEvent.BackClicked -> {
                val currentState = _state.value
                if (currentState.cartStatus == EDITABLE) {
                    return
                }
                _state.value = currentState.copy(cartStatus = EDITABLE)

                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                viewModelScope.launch {
                    analyticsTracker.track(BackToCartTapped)
                }
            }

            WooPosCartUIEvent.ClearAllClicked -> {
                viewModelScope.launch { analyticsTracker.track(ClearCartTapped) }
                val currentState = _state.value
                _state.value = currentState.copy(
                    body = WooPosCartState.Body.Empty
                )
            }
        }
    }

    private fun goToTotals() {
        val itemClickedDataList = (_state.value.body as WooPosCartState.Body.WithItems).itemsInCart.map {
            when (it) {
                is WooPosCartItemViewState.Product.Simple -> WooPosItemsViewModel.ItemClickedData.Product.Simple(it.id)
                is WooPosCartItemViewState.Product.Variation -> WooPosItemsViewModel.ItemClickedData.Product.Variation(
                    productId = it.id,
                    id = it.variationId
                )
                is WooPosCartItemViewState.Coupon -> WooPosItemsViewModel.ItemClickedData.Coupon(it.id, it.name)
            }
        }
        sendEventToParent(ChildToParentEvent.CheckoutClicked(itemClickedDataList))
        _state.value = _state.value.copy(cartStatus = CHECKOUT)
        trackCheckoutTapped(itemClickedDataList.size)
    }

    private fun trackCheckoutTapped(itemsInCart: Int) {
        viewModelScope.launch {
            analyticsTracker.track(CheckoutTapped.apply { addProperties(mapOf("items_in_cart" to "$itemsInCart")) })
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> handleBackFromCheckoutToCartClicked()

                    is ParentToChildrenEvent.ItemClickedInProductSelector -> handleItemClickedInItemsSelector(event)

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> clearCart()

                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    is ParentToChildrenEvent.CheckoutClicked,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    ParentToChildrenEvent.SearchEvent.Started -> Unit
                }
            }
        }
    }

    private fun handleBackFromCheckoutToCartClicked() {
        _state.value = _state.value.copy(cartStatus = EDITABLE)
    }

    private fun handleItemClickedInItemsSelector(event: ParentToChildrenEvent.ItemClickedInProductSelector) {
        viewModelScope.launch {
            val itemClicked = async {
                when (event.itemData) {
                    is WooPosItemsViewModel.ItemClickedData.Product.Simple ->
                        handleSimpleProductClicked(event.itemData.id)
                    is WooPosItemsViewModel.ItemClickedData.Product.Variation ->
                        handleVariationClicked(event.itemData.productId, event.itemData.id)
                    is WooPosItemsViewModel.ItemClickedData.Coupon ->
                        handleCouponClicked(event.itemData.id, event.itemData.couponCode)
                }
            }

            if (_state.value.body == WooPosCartState.Body.Empty) {
                analyticsTracker.track(InteractionWithCustomerStarted)
            }
            _state.value = updateStateWithNewItem(itemClicked.await())
            WooPosAnalyticsEvent.Event.ItemAddedToCart.addProperties(
                mapOf(
                    WooPosAnalyticsEventConstant.PRODUCT_TYPE to event.itemData.posItemNameForAnalytics()
                )
            )
            analyticsTracker.track(WooPosAnalyticsEvent.Event.ItemAddedToCart)
        }
    }

    private suspend fun handleSimpleProductClicked(productId: Long): WooPosCartItemViewState {
        val product = getProductById(productId)!!
        val itemNumber = getItemNumber()
        return product.toCartListItem(itemNumber)
    }

    private suspend fun handleVariationClicked(productId: Long, variationId: Long): WooPosCartItemViewState {
        val product = getProductById(productId)!!
        val itemNumber = getItemNumber()
        val productVariation = getVariationsById(productId, variationId)!!
        return productVariation.toCartListItem(itemNumber, product)
    }

    private fun handleCouponClicked(couponId: Long, couponCode: String): WooPosCartItemViewState {
        return WooPosCartItemViewState.Coupon(
            itemNumber = getItemNumber(),
            id = couponId,
            name = couponCode
        )
    }

    private fun clearCart() {
        _state.value = WooPosCartState()
    }

    private fun getItemNumber(): Int {
        return when (_state.value.body) {
            is WooPosCartState.Body.Empty -> 1
            is WooPosCartState.Body.WithItems -> itemNumberProvider.incrementAndGet()
        }
    }

    private fun updateStateWithNewItem(newItem: WooPosCartItemViewState): WooPosCartState {
        return when (val currentState = _state.value.body) {
            is WooPosCartState.Body.Empty -> _state.value.copy(body = WooPosCartState.Body.WithItems(listOf(newItem)))
            is WooPosCartState.Body.WithItems -> _state.value.copy(
                body = currentState.copy(itemsInCart = listOf(newItem) + currentState.itemsInCart)
            )
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
                    backIconVisible = false,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = newState.body is WooPosCartState.Body.WithItems
                )
            }

            CHECKOUT -> {
                WooPosCartState.Toolbar(
                    backIconVisible = true,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = false
                )
            }

            EMPTY -> {
                WooPosCartState.Toolbar(
                    backIconVisible = false,
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

    private fun updateAnalyticsData(newState: WooPosCartState) {
        if (newState.body == WooPosCartState.Body.Empty) {
            analyticsTrackingDataKeeper.reset()
            analyticsTrackingDataKeeper.interactionWithCustomerStartedTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun Product.toCartListItem(itemNumber: Int): WooPosCartItemViewState.Product.Simple =
        WooPosCartItemViewState.Product.Simple(
            itemNumber = itemNumber,
            id = this.remoteId,
            name = name,
            description = null,
            price = formatPrice(price),
            imageUrl = firstImageUrl,
        )

    private suspend fun ProductVariation.toCartListItem(
        itemNumber: Int,
        product: Product
    ): WooPosCartItemViewState.Product.Variation =
        WooPosCartItemViewState.Product.Variation(
            itemNumber = itemNumber,
            id = product.remoteId,
            variationId = this.remoteVariationId,
            name = product.name,
            description = getNameForPOS(product, resourceProvider),
            price = formatPrice(price),
            imageUrl = image?.source,
        )

    private fun getInitialValueOrHighestUsedItemNumberAfterProcessDeath() =
        (_state.value.body as? WooPosCartState.Body.WithItems)?.itemsInCart?.maxOfOrNull { it.itemNumber } ?: 1
}

private fun WooPosItemsViewModel.ItemClickedData.posItemNameForAnalytics(): String {
    return when (this) {
        is WooPosItemsViewModel.ItemClickedData.Product.Simple -> "simple"
        is WooPosItemsViewModel.ItemClickedData.Product.Variation -> "variation"
        is WooPosItemsViewModel.ItemClickedData.Coupon -> "coupon"
    }
}
