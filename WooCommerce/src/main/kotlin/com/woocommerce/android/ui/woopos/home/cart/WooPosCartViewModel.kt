package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    savedState: SavedStateHandle
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
                createOrder()
            }

            is WooPosCartUIEvent.BackFromCheckoutToCartClicked -> {
                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                _state.update { state ->
                    WooPosCartState.Cart(state.itemsInCart)
                }
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                sendEventToParent(ChildToParentEvent.ItemRemovedFromCart(event.item))
                _state.update { state ->
                    val itemsInCart = state.itemsInCart - event.item
                    when (state) {
                        is WooPosCartState.Cart -> state.copy(itemsInCart = itemsInCart)
                        is WooPosCartState.Checkout -> state.copy(itemsInCart = itemsInCart)
                    }
                }
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

                    is ParentToChildrenEvent.ProductSelectionChangedInProductSelector -> {
                        _state.update { state ->
                            when (state) {
                                is WooPosCartState.Cart -> state.copy(itemsInCart = event.selectedItems)
                                is WooPosCartState.Checkout -> state.copy(itemsInCart = event.selectedItems)
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

    private fun createOrder() {
        val itemsInCart = (state.value as? WooPosCartState)?.itemsInCart ?: return
        val products = itemsInCart.map { cartItem ->
            Order.Item(
                itemId = 0,
                productId = cartItem.productId,
                name = cartItem.title,
                price = 0.toBigDecimal(),
                sku = "",
                quantity = 01f,
                subtotal = 1.toBigDecimal(),
                totalTax = BigDecimal.ZERO,
                total = 10.toBigDecimal(),
                variationId = 0,
                attributesList = listOf()
            )
        }
        val order = buildOrder(products)

        viewModelScope.launch {
            if (!order.isEmpty()) {
                // orderCreateEditViewModel.onCreateOrderClicked(order)
            }
        }
    }

    private fun buildOrder(products: List<Order.Item>): Order {
        return Order(
            id = 0,
            number = "",
            dateCreated = Date(),
            dateModified = Date(),
            datePaid = null,
            status = Order.Status.Pending,
            total = calculateTotal(products),
            productsTotal = calculateSubtotal(products),
            totalTax = BigDecimal.ZERO, // Placeholder, needs to be updated by API response
            shippingTotal = BigDecimal.ZERO, // Placeholder
            discountTotal = BigDecimal.ZERO, // Placeholder
            refundTotal = BigDecimal.ZERO, // Placeholder
            currency = "USD", // Placeholder
            orderKey = "",
            customerNote = "",
            discountCodes = "",
            paymentMethod = "direct", // Placeholder
            paymentMethodTitle = "Direct Bank Transfer", // Placeholder
            isCashPayment = false,
            pricesIncludeTax = false,
            customer = null,
            shippingMethods = listOf(),
            items = products,
            shippingLines = listOf(),
            feesLines = listOf(),
            couponLines = listOf(), // Populate
            taxLines = listOf(), // Populate
            chargeId = null, // Populate
            shippingPhone = "", // Populate
            paymentUrl = "", // Populate
            isEditable = true,
            selectedGiftCard = null,
            giftCardDiscountedAmount = null,
            shippingTax = BigDecimal.ZERO
        )
    }

    private fun calculateSubtotal(products: List<Order.Item>): BigDecimal {
        return products.fold(BigDecimal.ZERO) { acc, product ->
            acc + product.subtotal
        }
    }

    private fun calculateTotal(products: List<Order.Item>): BigDecimal {
        return calculateSubtotal(products)
    }
}
