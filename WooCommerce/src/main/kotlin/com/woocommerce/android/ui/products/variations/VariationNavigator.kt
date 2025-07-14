package com.woocommerce.android.ui.products.variations

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphProductsDirections
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewAttributes
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewDescriptionEditor
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewImageGallery
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewInventory
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewMediaUploadErrors
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewPricing
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewProductQuantityRules
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewProductSubscriptionExpiration
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewShipping
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewVariationSubscriptionTrial
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
    @Suppress("LongMethod")
    fun navigate(fragment: Fragment, target: VariationNavigationTarget) {
        when (target) {
            is ViewImageGallery -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToNavGraphImageGallery(
                    remoteId = target.remoteId,
                    images = target.images.toTypedArray(),
                    selectedImage = target.selectedImage,
                    showChooser = target.showChooser,
                    requestCode = RequestCodes.VARIATION_DETAIL_IMAGE
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewDescriptionEditor -> {
                val action = VariationDetailFragmentDirections
                    .actionGlobalAztecEditorFragment(
                        aztecText = target.description,
                        aztecTitle = target.title,
                        aztecCaption = null
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewPricing -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductPricingFragment(
                    requestCode = RequestCodes.VARIATION_DETAIL_PRICING,
                    pricingData = target.pricingData
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewInventory -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductInventoryFragment(
                    requestCode = RequestCodes.VARIATION_DETAIL_INVENTORY,
                    inventoryData = target.inventoryData,
                    sku = target.sku
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShipping -> {
                val action = VariationDetailFragmentDirections.actionVariationDetailFragmentToProductShippingFragment(
                    requestCode = RequestCodes.VARIATION_DETAIL_SHIPPING,
                    shippingData = target.shippingData
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewAttributes -> {
                VariationDetailFragmentDirections
                    .actionVariationDetailFragmentToEditVariationAttributesFragment(
                        remoteProductId = target.remoteProductId,
                        remoteVariationId = target.remoteVariationId
                    ).let { fragment.findNavController().navigateSafely(it) }
            }
            is ViewMediaUploadErrors -> {
                val action = NavGraphProductsDirections.actionGlobalMediaUploadErrorsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductQuantityRules -> {
                val action = VariationDetailFragmentDirections
                    .actionVariationDetailFragmentToProductQuantityRulesFragment(
                        target.quantityRules,
                        target.exitAnalyticsEvent
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductSubscriptionExpiration -> {
                val action = VariationDetailFragmentDirections
                    .actionVariationDetailFragmentToProductSubscriptionExpirationFragment(target.subscription)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewVariationSubscriptionTrial -> {
                val action = VariationDetailFragmentDirections
                    .actionVariationDetailFragmentToProductSubscriptionFreeTrialFragment(target.subscription)
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
