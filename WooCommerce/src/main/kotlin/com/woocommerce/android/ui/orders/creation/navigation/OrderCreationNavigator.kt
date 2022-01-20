package com.woocommerce.android.ui.orders.creation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreationFormFragmentDirections
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionFragmentDirections

object OrderCreationNavigator {
    fun navigate(fragment: Fragment, target: OrderCreationNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            is EditCustomer ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment()
            is EditCustomerNote ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
            is AddProduct ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationProductSelectionFragment()
            is ShowProductDetails ->
                OrderCreationFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationProductDetailsFragment(target.item)
            is ShowProductVariations ->
                OrderCreationProductSelectionFragmentDirections
                    .actionOrderCreationProductSelectionFragmentToOrderCreationVariationSelectionFragment(
                        target.productId
                    )
            is ShowCreatedOrder ->
                OrderCreationFormFragmentDirections
                    .actionOrderCreationFragmentToOrderDetailFragment(target.orderId)
        }

        navController.navigate(action)
    }
}
