package com.woocommerce.android.ui.orders

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderNavigationTarget) {
        when (target) {
            is ViewOrderStatusSelector -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderStatusSelectorDialog(
                        target.currentStatus, target.orderStatusList
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is IssueOrderRefund -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToIssueRefund(target.remoteOrderId)
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
