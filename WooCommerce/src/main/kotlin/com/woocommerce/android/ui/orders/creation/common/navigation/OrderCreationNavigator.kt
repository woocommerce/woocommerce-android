package com.woocommerce.android.ui.orders.creation.common.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigationTarget.AddCustomer
import com.woocommerce.android.ui.orders.creation.neworder.NewOrderFragmentDirections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderCreationNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderCreationNavigationTarget) {
        when (target) {
            is AddCustomer -> {
                val action = NewOrderFragmentDirections.actionNewOrderFragmentToAddCustomerFragment()
                fragment.findNavController().navigateSafely(action)
            }
        }.exhaustive
    }
}
