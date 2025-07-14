package com.woocommerce.android.ui.orders.creation.configuration

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.viewmodel.MultiLiveEvent

sealed class ProductConfigurationNavigationTarget : MultiLiveEvent.Event() {
    data class NavigateToVariationSelector(
        val itemId: Long,
        val productId: Long,
        val variationRule: VariableProductRule
    ) : ProductConfigurationNavigationTarget()
}

object ProductConfigurationNavigator {
    fun navigate(fragment: Fragment, target: ProductConfigurationNavigationTarget) {
        val navController = fragment.findNavController()
        when (target) {
            is ProductConfigurationNavigationTarget.NavigateToVariationSelector -> {
                navController.navigateSafely(
                    ProductConfigurationFragmentDirections.actionProductConfigurationFragmentToVariationPickerFragment(
                        productId = target.productId,
                        allowedVatiations = target.variationRule.variationIds?.toLongArray(),
                        itemId = target.itemId
                    )
                )
            }
        }
    }
}
