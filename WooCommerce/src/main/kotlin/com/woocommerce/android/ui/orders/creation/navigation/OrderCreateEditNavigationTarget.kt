package com.woocommerce.android.ui.orders.creation.navigation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED
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
        val restrictions: List<ProductSelectorViewModel.ProductSelectorRestriction>,
        val productSelectedAnalyticsEvent: AnalyticsEvent = ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED,
        val productUnselectedAnalyticsEvent: AnalyticsEvent = ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED,
        val confirmButtonTappedAnalyticsEvent: AnalyticsEvent = ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED
    ) : OrderCreateEditNavigationTarget()
    data class ShowProductDetails(val item: Order.Item) : OrderCreateEditNavigationTarget()
    data class ShowCreatedOrder(val orderId: Long) : OrderCreateEditNavigationTarget()
    data class EditShipping(val currentShippingLine: ShippingLine?) : OrderCreateEditNavigationTarget()
    data class EditFee(
        val orderSubTotal: BigDecimal,
        val currentFeeValue: BigDecimal? = null
    ) : OrderCreateEditNavigationTarget()
}
