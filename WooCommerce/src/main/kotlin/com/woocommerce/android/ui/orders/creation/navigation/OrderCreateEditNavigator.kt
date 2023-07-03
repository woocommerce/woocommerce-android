package com.woocommerce.android.ui.orders.creation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreateEditFormFragmentDirections
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCoupon
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.SelectItems
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.util.FeatureFlag

object OrderCreateEditNavigator {
    fun navigate(fragment: Fragment, target: OrderCreateEditNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            is EditCustomer -> {
                if (FeatureFlag.CUSTOMER_LIST_SEARCH_2.isEnabled()) {
                    OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToCustomerListFragment()
                } else {
                    OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment()
                }
            }
            is EditCustomerNote ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
            is SelectItems ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToProductSelectorFragment(
                    selectedItems = target.selectedItems.toTypedArray(),
                    restrictions = target.restrictions.toTypedArray(),
                    productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation
                )
            is EditFee ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationEditFeeFragment(
                    orderSubTotal = target.orderSubTotal,
                    currentFeeValue = target.currentFeeValue
                )
            is ShowProductDetails ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationProductDetailsFragment(target.item)
            is ShowCreatedOrder ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderDetailFragment(target.orderId)
            is EditShipping ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationShippingFragment(target.currentShippingLine)
            is EditCoupon ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCouponEditFragment(
                    orderCreationMode = target.orderCreationMode,
                    couponCode = target.couponCode
                )
            is OrderCreateEditNavigationTarget.CouponList -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCouponListFragment(
                    orderCreationMode = target.orderCreationMode,
                    couponLines = target.couponLines.toTypedArray()
                )
            }
        }

        navController.navigate(action)
    }
}
