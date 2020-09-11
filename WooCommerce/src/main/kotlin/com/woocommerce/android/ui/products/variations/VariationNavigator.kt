package com.woocommerce.android.ui.products.variations

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewDescriptionEditor
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewImageGallery
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewInventory
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewPricing
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewShipping
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class that handles navigation logic for variation-related screens.
 * Modify the [navigate] method when you want to add more logic to navigate
 * the Variation detail/variation sub detail screen.
 *
 * Note that a new data class in the [VariationNavigationTarget] must be added first
 */
@Singleton
class VariationNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: VariationNavigationTarget) {
        when (target) {
            is ViewImageGallery -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToNavGraphImageGallery(
                    target.remoteId,
                    target.images.toTypedArray(),
                    target.selectedImage,
                    target.showChooser,
                    RequestCodes.VARIATION_DETAIL_IMAGE
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewDescriptionEditor -> {
                val action = VariationDetailFragmentDirections
                    .actionGlobalAztecEditorFragment(
                        target.description,
                        target.title,
                        null,
                        RequestCodes.AZTEC_EDITOR_VARIATION_DESCRIPTION
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewPricing -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductPricingFragment(
                    RequestCodes.VARIATION_DETAIL_PRICING,
                    target.pricingData
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewInventory -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductInventoryFragment(
                    RequestCodes.VARIATION_DETAIL_INVENTORY,
                    target.inventoryData,
                    target.sku
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShipping -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductShippingFragment(
                    RequestCodes.VARIATION_DETAIL_SHIPPING,
                    target.shippingData
                )
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
