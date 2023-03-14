package com.woocommerce.android.ui.orders.creation.navigation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import java.math.BigDecimal

sealed class OrderCreateEditNavigationTarget : Event() {
    object EditCustomer : OrderCreateEditNavigationTarget()
    object EditCustomerNote : OrderCreateEditNavigationTarget()
    data class SelectItems(
        val selectedItems: List<ProductSelectorViewModel.SelectedItem>,
        val restrictions: List<ProductSelectorViewModel.ProductSelectorRestriction>
    ) : OrderCreateEditNavigationTarget()
    data class ShowProductDetails(val item: Order.Item) : OrderCreateEditNavigationTarget()
    data class ShowCreatedOrder(val orderId: Long) : OrderCreateEditNavigationTarget()
    data class EditShipping(val currentShippingLine: ShippingLine?) : OrderCreateEditNavigationTarget()
    data class EditFee(
        val orderSubTotal: BigDecimal,
        val currentFeeValue: BigDecimal? = null
    ) : OrderCreateEditNavigationTarget()
}
