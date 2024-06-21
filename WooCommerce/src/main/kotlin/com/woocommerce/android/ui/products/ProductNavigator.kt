package com.woocommerce.android.ui.products

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.NavGraphProductsDirections
import com.woocommerce.android.R.id
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.orders.creation.configuration.Flow
import com.woocommerce.android.ui.products.AddProductSource.STORE_ONBOARDING
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragmentDirections
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailFragmentDirections
import com.woocommerce.android.ui.products.downloads.ProductDownloadsFragmentDirections
import com.woocommerce.android.ui.products.grouped.GroupedProductListType.GROUPED
import com.woocommerce.android.ui.products.selector.ProductSelectorFragmentDirections
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.settings.ProductSettingsFragmentDirections
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsFragmentDirections
import com.woocommerce.android.ui.products.variations.attributes.AttributeListFragmentDirections
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
            is ProductNavigationTarget.ShareProduct -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, target.title)
                    putExtra(Intent.EXTRA_TEXT, target.url)
                    type = "text/plain"
                }
                val title = fragment.resources.getText(string.product_share_dialog_title)
                fragment.startActivity(Intent.createChooser(shareIntent, title))
            }

            is ProductNavigationTarget.ShareProductWithMessage -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, target.subject)
                    putExtra(Intent.EXTRA_TITLE, target.title)
                    type = "text/plain"
                }
                val title = fragment.resources.getText(string.product_share_dialog_title)
                fragment.startActivity(Intent.createChooser(shareIntent, title))
            }

            is ProductNavigationTarget.ShareProductWithAI -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductSharingFragment(
                        target.permalink,
                        target.title,
                        target.description
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductVariations -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToVariationListFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductAttributes -> {
                ProductDetailFragmentDirections
                    .actionProductDetailFragmentToAttributeListFragment()
                    .apply { fragment.findNavController().navigateSafely(this) }
            }

            is ProductNavigationTarget.ViewProductDescriptionEditor -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalAztecEditorFragment(
                        target.description,
                        target.title,
                        null,
                        RequestCodes.AZTEC_EDITOR_PRODUCT_DESCRIPTION,
                        target.productTitle
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductShortDescriptionEditor -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalAztecEditorFragment(
                        target.shortDescription,
                        target.title,
                        null,
                        RequestCodes.AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductPurchaseNoteEditor -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalAztecEditorFragment(
                        target.purchaseNote,
                        target.title,
                        target.caption
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductInventory -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductInventoryFragment(
                        RequestCodes.PRODUCT_DETAIL_INVENTORY,
                        target.inventoryData,
                        target.sku,
                        target.productType
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductPricing -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductPricingFragment(
                        RequestCodes.PRODUCT_DETAIL_PRICING,
                        target.pricingData
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductExternalLink -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductExternalLinkFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductShipping -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductShippingFragment(
                        RequestCodes.PRODUCT_DETAIL_SHIPPING,
                        target.shippingData
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductSettings -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductSettingsFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductStatus -> {
                val status = target.status?.toString() ?: ""
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductStatusFragment(status)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductCatalogVisibility -> {
                val catalogVisibility = target.catalogVisibility?.toString() ?: ""
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductCatalogVisibilityFragment(
                        catalogVisibility,
                        target.isFeatured
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductVisibility -> {
                val visibility = target.visibility.toString()
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductVisibilityFragment(
                        target.isApplicationPasswordsLogin,
                        visibility,
                        target.password
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductSlug -> {
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductSlugFragment(target.slug)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductImageGallery -> {
                viewProductImages(fragment, target.remoteId, target.images, target.selectedImage, target.showChooser)
            }

            is ProductNavigationTarget.ViewProductMenuOrder -> {
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductMenuOrderFragment(target.menuOrder)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductCategories -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductCategoriesFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.AddProductCategory -> {
                val action = ProductCategoriesFragmentDirections
                    .actionProductCategoriesFragmentToAddProductCategoryFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.EditCategory -> {
                val action = ProductCategoriesFragmentDirections
                    .actionProductCategoriesFragmentToEditProductCategoryFragment(
                        productCategory = target.category
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductTags -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductTagsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductDetailBottomSheet -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductDetailBottomSheetFragment(target.productType)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductTypes -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductTypesBottomSheetFragment(
                        target.isAddProduct,
                        currentProductType = target.currentProductType,
                        isCurrentProductVirtual = target.isCurrentProductVirtual
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductReviews -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductReviewsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewGroupedProducts -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalGroupedProductListFragment(
                        remoteProductId = target.remoteId,
                        productIds = target.groupedProductIds.toLongArray(),
                        groupedProductListType = GROUPED
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewLinkedProducts -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToLinkedProductsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductSelectionList -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductSelectionListFragment(
                        remoteProductId = target.remoteId,
                        groupedProductListType = target.groupedProductType,
                        excludedProductIds = target.excludedProductIds.toLongArray()
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductAdd -> {
                val directions = NavGraphMainDirections.actionGlobalProductDetailFragment(
                    mode = ProductDetailFragment.Mode.AddNewProduct,
                    source = target.source
                )

                fragment.findNavController().navigateSafely(
                    directions = directions,
                    navOptions =
                    if (target.source == STORE_ONBOARDING) {
                        NavOptions.Builder()
                            .setPopUpTo(id.dashboard, false)
                            .build()
                    } else {
                        null
                    }
                )
            }

            is ProductNavigationTarget.ViewProductDownloads -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductDownloadsFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductDownloadsSettings -> {
                val action = ProductDownloadsFragmentDirections
                    .actionProductDownloadsFragmentToProductDownloadsSettingsFragment()

                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductDownloadDetails -> {
                val action = NavGraphProductsDirections
                    .actionGlobalProductDownloadDetailsFragment(target.isEditing, target.file)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductAddonsDetails -> {
                ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductAddonsFragment()
                    .apply { fragment.findNavController().navigateSafely(this) }
            }

            is ProductNavigationTarget.AddProductDownloadableFile -> {
                val action = NavGraphProductsDirections.actionGlobalAddProductDownloadBottomSheetFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.AddProductAttribute -> {
                when (target.isVariationCreation) {
                    true ->
                        ProductDetailFragmentDirections
                            .actionProductDetailFragmentToAddAttributeFragment(isVariationCreation = true)
                            .run { fragment.findNavController().navigateSafely(this) }

                    else ->
                        AttributeListFragmentDirections
                            .actionAttributeListFragmentToAddAttributeFragment()
                            .run { fragment.findNavController().navigateSafely(this) }
                }
            }

            is ProductNavigationTarget.RenameProductAttribute -> {
                val action = AddAttributeTermsFragmentDirections
                    .actionAttributeTermsFragmentToRenameAttributeFragment(target.attributeName)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.AddProductAttributeTerms -> {
                val action = NavGraphProductsDirections.actionGlobalAddVariationAttributeTermsFragment(
                    attributeId = target.attributeId,
                    attributeName = target.attributeName,
                    isNewAttribute = target.isNewAttribute,
                    isVariationCreation = target.isVariationCreation
                )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewMediaUploadErrors -> {
                val action = NavGraphProductsDirections.actionGlobalMediaUploadErrorsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.NavigateToVariationSelector -> {
                val action = when (target.selectionMode) {
                    ProductSelectorViewModel.SelectionMode.MULTIPLE -> {
                        ProductSelectorFragmentDirections.actionProductSelectorFragmentToVariationSelectorFragment(
                            productId = target.productId,
                            variationIds = target.selectedVariationIds.toLongArray(),
                            productSelectorFlow = target.productSelectorFlow,
                            productSource = target.productSourceForTracking,
                            screenMode = target.screenMode
                        )
                    }

                    ProductSelectorViewModel.SelectionMode.SINGLE -> {
                        ProductSelectorFragmentDirections.actionProductSelectorFragmentToVariationPickerFragment(
                            productId = target.productId
                        )
                    }
                    ProductSelectorViewModel.SelectionMode.LIVE -> {
                        ProductSelectorFragmentDirections.actionProductSelectorFragmentToVariationSelectorFragment(
                            productId = target.productId,
                            variationIds = target.selectedVariationIds.toLongArray(),
                            productSelectorFlow = target.productSelectorFlow,
                            productSource = target.productSourceForTracking,
                            screenMode = target.screenMode
                        )
                    }
                }

                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.NavigateToProductConfiguration -> {
                val flow = Flow.Selection(target.productId)
                fragment.findNavController().navigateSafely(
                    ProductSelectorFragmentDirections.actionProductSelectorFragmentToProductConfigurationFragment(flow)
                )
            }

            is ProductNavigationTarget.EditProductConfiguration -> {
                val flow = Flow.Edit(
                    itemId = target.itemId,
                    productID = target.productId,
                    configuration = target.configuration
                )
                fragment.findNavController().navigateSafely(
                    ProductSelectorFragmentDirections.actionProductSelectorFragmentToProductConfigurationFragment(flow)
                )
            }

            is ProductNavigationTarget.NavigateToProductFilter -> {
                fragment.findNavController().navigateSafely(
                    ProductSelectorFragmentDirections.actionProductSelectorFragmentToNavGraphProductFilters(
                        target.stockStatus,
                        target.productType,
                        target.productStatus,
                        target.productCategory,
                        target.productCategoryName,
                    )
                )
            }

            is ProductNavigationTarget.ViewProductSubscriptionExpiration -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductSubscriptionExpirationFragment(
                        target.subscription
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductSubscriptionFreeTrial -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductSubscriptionFreeTrialFragment(
                        target.subscription
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductQuantityRules -> {
                val action = ProductDetailFragmentDirections.actionProductDetailFragmentToProductQuantityRulesFragment(
                    target.quantityRules,
                    target.exitAnalyticsEvent
                )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewBundleProducts -> {
                ProductDetailFragmentDirections.actionProductDetailFragmentToProductBundleFragment(
                    target.productId
                ).let { fragment.findNavController().navigateSafely(it) }
            }

            is ProductNavigationTarget.ViewProductComponents -> {
                val action = ProductDetailFragmentDirections.actionProductDetailFragmentToCompositeProductFragment(
                    target.components.toTypedArray()
                )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ExitProduct -> fragment.findNavController().navigateUp()

            is ProductNavigationTarget.ViewFirstProductCelebration -> {
                val action = ProductDetailFragmentDirections.actionProductDetailFragmentToFirstProductCelebrationDialog(
                    productName = target.productName,
                    permalink = target.permalink
                )
                fragment.findNavController().navigateSafely(action)
            }
        }
    }

    private fun viewProductImages(
        fragment: Fragment,
        remoteId: Long,
        images: List<Image>,
        selectedImage: Image?,
        showChooser: Boolean
    ) {
        val action = ProductDetailFragmentDirections.actionProductDetailFragmentToNavGraphImageGallery(
            remoteId = remoteId,
            images = images.toTypedArray(),
            selectedImage = selectedImage,
            showChooser = showChooser,
            requestCode = RequestCodes.PRODUCT_DETAIL_IMAGES
        )
        fragment.findNavController().navigateSafely(action)
    }
}
