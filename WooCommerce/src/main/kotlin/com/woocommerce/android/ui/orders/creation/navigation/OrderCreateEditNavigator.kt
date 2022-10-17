package com.woocommerce.android.ui.orders.creation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreateEditFormFragmentDirections
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.AddProduct
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.orders.creation.products.OrderCreateEditProductSelectionFragmentDirections

object OrderCreateEditNavigator {
    fun navigate(fragment: Fragment, target: OrderCreateEditNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            is EditCustomer ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment()
            is EditCustomerNote ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
            is AddProduct ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationProductSelectionFragment()
            is EditFee ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationEditFeeFragment(
                    orderSubTotal = target.orderSubTotal,
                    currentFeeValue = target.currentFeeValue
                )
            is ShowProductDetails ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationProductDetailsFragment(target.item)
            is ShowProductVariations ->
                OrderCreateEditProductSelectionFragmentDirections
                    .actionOrderCreationProductSelectionFragmentToOrderCreationVariationSelectionFragment(
                        target.productId
                    )
            is ShowCreatedOrder ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderDetailFragment(target.orderId)
            is EditShipping ->
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationShippingFragment(target.currentShippingLine)
        }

        navController.navigate(action)
    }
}
