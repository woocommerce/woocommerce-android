package com.woocommerce.android.ui.orders.creation.navigation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class OrderCreationNavigationTarget : Event() {
    object EditCustomer : OrderCreationNavigationTarget()
    object EditCustomerNote : OrderCreationNavigationTarget()
    object AddProduct : OrderCreationNavigationTarget()
    object EditFee : OrderCreationNavigationTarget()
    data class ShowProductVariations(val productId: Long) : OrderCreationNavigationTarget()
    data class ShowProductDetails(val item: Order.Item) : OrderCreationNavigationTarget()
    data class ShowCreatedOrder(val orderId: Long) : OrderCreationNavigationTarget()
    data class EditShipping(val currentShippingLine: ShippingLine?) : OrderCreationNavigationTarget()
}
