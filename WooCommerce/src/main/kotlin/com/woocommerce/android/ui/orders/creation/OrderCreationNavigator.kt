package com.woocommerce.android.ui.orders.creation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionFragmentDirections

object OrderCreationNavigator {
    fun navigate(fragment: Fragment, target: OrderCreationNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            EditCustomer ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment()
            EditCustomerNote ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
            AddProduct ->
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
