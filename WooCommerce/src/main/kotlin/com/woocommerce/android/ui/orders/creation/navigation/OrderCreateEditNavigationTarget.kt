package com.woocommerce.android.ui.orders.creation.navigation

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.ui.orders.creation.CustomAmountUIModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.AutoTaxRateSettingState
import com.woocommerce.android.ui.orders.creation.taxes.TaxRatesInfoDialogViewState
import com.woocommerce.android.ui.products.ProductRestriction
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import java.math.BigDecimal

sealed class OrderCreateEditNavigationTarget : Event() {
    object AddCustomer : OrderCreateEditNavigationTarget()
    object EditCustomer : OrderCreateEditNavigationTarget()
    object EditCustomerNote : OrderCreateEditNavigationTarget()
    data class SelectItems(
        val selectedItems: List<ProductSelectorViewModel.SelectedItem>,
        val restrictions: List<ProductRestriction>
    ) : OrderCreateEditNavigationTarget()

    data class ShowCreatedOrder(val orderId: Long) : OrderCreateEditNavigationTarget()
    data class EditShipping(val currentShippingLine: ShippingLine?) :
        OrderCreateEditNavigationTarget()

    data class EditFee(
        val orderSubTotal: BigDecimal,
        val currentFeeValue: BigDecimal? = null
    ) : OrderCreateEditNavigationTarget()

    data class EditCoupon(
        val orderCreationMode: OrderCreateEditViewModel.Mode,
        val couponCode: String? = null
    ) : OrderCreateEditNavigationTarget()

    object AddCoupon : OrderCreateEditNavigationTarget()

    data class CouponList(
        val orderCreationMode: OrderCreateEditViewModel.Mode,
        val couponLines: Collection<Order.CouponLine>
    ) : OrderCreateEditNavigationTarget()

    data class TaxRatesInfoDialog(val state: TaxRatesInfoDialogViewState) :
        OrderCreateEditNavigationTarget()

    data class TaxRateSelector(val state: TaxRatesInfoDialogViewState) :
        OrderCreateEditNavigationTarget()

    data class AutoTaxRateSettingDetails(val state: AutoTaxRateSettingState) :
        OrderCreateEditNavigationTarget()

    data class EditDiscount(
        val item: Order.Item,
        val currency: String,
    ) : OrderCreateEditNavigationTarget()

    data class CustomAmountDialog(
        val customAmountUIModel: CustomAmountUIModel? = null
    ) : OrderCreateEditNavigationTarget()
}
