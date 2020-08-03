package com.woocommerce.android.ui.products

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductCategory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCatalogVisibility
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDetailBottomSheet
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductExternalLink
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImageChooser
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImages
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductMenuOrder
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPurchaseNoteEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSlug
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVisibility
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragmentDirections
import com.woocommerce.android.ui.products.settings.ProductSettingsFragmentDirections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class that handles navigation logic for product related screens.
 * Currently injected to [BaseProductFragment]. Modify the [navigate] method when you want to add more logic
 * to navigate the Products detail/product sub detail screen.
 *
 * Note that a new data class in the [ProductNavigationTarget] must be added first
 */
@Singleton
class ProductNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: ProductNavigationTarget) {
        when (target) {
            is ShareProduct -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, target.title)
                    putExtra(Intent.EXTRA_TEXT, target.url)
                    type = "text/plain"
                }
                val title = fragment.resources.getText(R.string.product_share_dialog_title)
                fragment.startActivity(Intent.createChooser(shareIntent, title))
            }

            is ViewProductVariations -> {
                val action = ProductDetailFragmentDirections
                        .actionProductDetailFragmentToVariationListFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductDescriptionEditor -> {
                val action = ProductDetailFragmentDirections
                        .actionGlobalAztecEditorFragment(
                                target.description,
                                target.title,
                                null,
                                RequestCodes.AZTEC_EDITOR_PRODUCT_DESCRIPTION
                        )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductShortDescriptionEditor -> {
                val action = ProductDetailFragmentDirections
                        .actionGlobalAztecEditorFragment(
                                target.shortDescription,
                                target.title,
                                null,
                                RequestCodes.AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION
                        )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductPurchaseNoteEditor -> {
                val action = ProductDetailFragmentDirections
                        .actionGlobalAztecEditorFragment(
                                target.purchaseNote,
                                target.title,
                                target.caption,
                                RequestCodes.PRODUCT_SETTINGS_PURCHASE_NOTE
                        )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductInventory -> {
                val action = ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductInventoryFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductPricing -> {
                val action = ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductPricingFragment(
                            RequestCodes.PRODUCT_DETAIL_PRICING,
                            target.pricingData
                        )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductExternalLink -> {
                val action = ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductExternalLinkFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductShipping -> {
                val action = ProductDetailFragmentDirections
                        .actionGlobalProductShippingFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductImageChooser -> viewProductImageChooser(fragment, target.remoteId)

            is ViewProductSettings -> {
                val action = ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductSettingsFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductStatus -> {
                val status = target.status?.toString() ?: ""
                val action = ProductSettingsFragmentDirections
                        .actionProductSettingsFragmentToProductStatusFragment(status)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductCatalogVisibility -> {
                val catalogVisibility = target.catalogVisibility?.toString() ?: ""
                val action = ProductSettingsFragmentDirections
                        .actionProductSettingsFragmentToProductCatalogVisibilityFragment(
                                catalogVisibility,
                                target.isFeatured
                        )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductVisibility -> {
                val visibility = target.visibility.toString()
                val action = ProductSettingsFragmentDirections
                        .actionProductSettingsFragmentToProductVisibilityFragment(visibility, target.password)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductSlug -> {
                val action = ProductSettingsFragmentDirections
                        .actionProductSettingsFragmentToProductSlugFragment(target.slug)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductImages -> {
                viewProductImageChooser(fragment, target.product.remoteId)
            }

            is ViewProductMenuOrder -> {
                val action = ProductSettingsFragmentDirections
                        .actionProductSettingsFragmentToProductMenuOrderFragment(target.menuOrder)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductCategories -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductCategoriesFragment(target.remoteId)
                fragment.findNavController().navigate(action)
            }

            is AddProductCategory -> {
                val action = ProductCategoriesFragmentDirections
                    .actionProductCategoriesFragmentToAddProductCategoryFragment()
                fragment.findNavController().navigate(action)
            }

            is ViewProductTags -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductTagsFragment(target.remoteId)
                fragment.findNavController().navigate(action)
            }

            is ViewProductDetailBottomSheet -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductDetailBottomSheetFragment(target.remoteId)
                fragment.findNavController().navigate(action)
            }

            is ExitProduct -> fragment.findNavController().navigateUp()
        }
    }

    private fun viewProductImageChooser(fragment: Fragment, remoteId: Long) {
        val action = ProductDetailFragmentDirections
                .actionProductDetailFragmentToProductImagesFragment(remoteId)
        fragment.findNavController().navigateSafely(action)
    }

    private fun viewProductImageViewer(fragment: Fragment, remoteId: Long) {
        val action = ProductImageViewerFragmentDirections
                .actionGlobalProductImageViewerFragment(remoteId)
        fragment.findNavController().navigateSafely(action)
    }
}
