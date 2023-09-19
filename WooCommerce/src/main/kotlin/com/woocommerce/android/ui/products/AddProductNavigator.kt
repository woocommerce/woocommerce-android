package com.woocommerce.android.ui.products

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class AddProductNavigator @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productListRepository: ProductListRepository
) {
    fun NavController.navigateToAddProducts(
        aiBottomSheetAction: NavDirections,
        typesBottomSheetAction: NavDirections
    ) {
        if (FeatureFlag.PRODUCT_CREATION_AI.isEnabled() &&
            selectedSite.get().isEligibleForAI &&
            !hasNonSampleProducts()
        ) {
            navigateSafely(aiBottomSheetAction)
        } else {
            navigateSafely(typesBottomSheetAction)
        }
    }

    private fun hasNonSampleProducts(): Boolean {
        return productListRepository.getProductList().filterNot { it.isSampleProduct }.isNotEmpty()
    }
}
