package com.woocommerce.android.ui.orders.creation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.*

object OrderCreationNavigator {
    fun navigate(fragment: Fragment, target: OrderCreationNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            EditCustomer ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment()
            EditCustomerNote ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
            AddSimpleProduct ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationProductSelectionFragment()
            is ShowProductDetails ->
                OrderCreationFormFragmentDirections
                    .actionOrderCreationFragmentToOrderCreationProductDetailsFragment(target.item)
        }

        navController.navigate(action)
    }
}
