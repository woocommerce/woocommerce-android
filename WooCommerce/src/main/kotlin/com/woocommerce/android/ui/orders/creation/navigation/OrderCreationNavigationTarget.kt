package com.woocommerce.android.ui.orders.creation.navigation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import java.math.BigDecimal

sealed class OrderCreationNavigationTarget : Event() {
    object EditCustomer : OrderCreationNavigationTarget()
    object EditCustomerNote : OrderCreationNavigationTarget()
    object AddProduct : OrderCreationNavigationTarget()
    data class ShowProductVariations(val productId: Long) : OrderCreationNavigationTarget()
    data class ShowProductDetails(val item: Order.Item) : OrderCreationNavigationTarget()
    data class ShowCreatedOrder(val orderId: Long) : OrderCreationNavigationTarget()
    data class EditShipping(val currentShippingLine: ShippingLine?) : OrderCreationNavigationTarget()
    data class EditFee(
        val orderSubTotal: BigDecimal,
        val currentFeeValue: BigDecimal? = null
    ) : OrderCreationNavigationTarget()
}
