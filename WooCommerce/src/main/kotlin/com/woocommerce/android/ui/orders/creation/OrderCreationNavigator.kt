package com.woocommerce.android.ui.orders.creation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.EditCustomerNote
import javax.inject.Inject

class OrderCreationNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderCreationNavigationTarget) {
        val navController = fragment.findNavController()

        val action = when (target) {
            is EditCustomerNote ->
                OrderCreationFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()
        }

        navController.navigate(action)
    }
}
