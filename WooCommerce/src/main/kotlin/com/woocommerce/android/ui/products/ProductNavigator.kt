package com.woocommerce.android.ui.products

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.NavGraphProductsDirections
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.AddProductSource.STORE_ONBOARDING
import com.woocommerce.android.ui.products.GroupedProductListType.GROUPED
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductAttribute
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductAttributeTerms
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductCategory
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductDownloadableFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToProductFilter
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToVariationSelector
import com.woocommerce.android.ui.products.ProductNavigationTarget.RenameProductAttribute
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProductWithAI
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProductWithMessage
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewGroupedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewLinkedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewMediaUploadErrors
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAdd
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAddonsDetails
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAttributes
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCatalogVisibility
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDetailBottomSheet
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloads
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadsSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductExternalLink
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImageGallery
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductMenuOrder
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPurchaseNoteEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductReviews
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSelectionList
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSlug
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTypes
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVisibility
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragmentDirections
import com.woocommerce.android.ui.products.downloads.ProductDownloadsFragmentDirections
import com.woocommerce.android.ui.products.selector.ProductSelectorFragmentDirections
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

            is ShareProductWithMessage -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, target.subject)
                    putExtra(Intent.EXTRA_TITLE, target.title)
                    type = "text/plain"
                }
                val title = fragment.resources.getText(R.string.product_share_dialog_title)
                fragment.startActivity(Intent.createChooser(shareIntent, title))
            }

            is ShareProductWithAI -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductSharingFragment(
                        target.permalink,
                        target.title,
                        target.description
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductVariations -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToVariationListFragment(
                        target.remoteId,
                        target.isReadOnlyMode
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductAttributes -> {
                ProductDetailFragmentDirections
                    .actionProductDetailFragmentToAttributeListFragment()
                    .apply { fragment.findNavController().navigateSafely(this) }
            }

            is ViewProductDescriptionEditor -> {
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
                        target.caption
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductInventory -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductInventoryFragment(
                        RequestCodes.PRODUCT_DETAIL_INVENTORY,
                        target.inventoryData,
                        target.sku,
                        target.productType
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductPricing -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductPricingFragment(
                        pricingData = target.pricingData,
                        requestCode = RequestCodes.PRODUCT_DETAIL_PRICING,
                        productName = target.productName,
                        productDescription = target.productDescription
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
                    .actionGlobalProductShippingFragment(
                        RequestCodes.PRODUCT_DETAIL_SHIPPING,
                        target.shippingData
                    )
                fragment.findNavController().navigateSafely(action)
            }

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
                    .actionProductSettingsFragmentToProductVisibilityFragment(
                        target.isApplicationPasswordsLogin,
                        visibility,
                        target.password
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductSlug -> {
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductSlugFragment(target.slug)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductImageGallery -> {
                viewProductImages(fragment, target.remoteId, target.images, target.selectedImage, target.showChooser)
            }

            is ViewProductMenuOrder -> {
                val action = ProductSettingsFragmentDirections
                    .actionProductSettingsFragmentToProductMenuOrderFragment(target.menuOrder)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductCategories -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductCategoriesFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is AddProductCategory -> {
                val action = ProductCategoriesFragmentDirections
                    .actionProductCategoriesFragmentToAddProductCategoryFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductTags -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductTagsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductDetailBottomSheet -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductDetailBottomSheetFragment(target.productType)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductTypes -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductTypesBottomSheetFragment(
                        target.isAddProduct,
                        currentProductType = target.currentProductType,
                        isCurrentProductVirtual = target.isCurrentProductVirtual
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductReviews -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductReviewsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewGroupedProducts -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalGroupedProductListFragment(
                        remoteProductId = target.remoteId,
                        productIds = target.groupedProductIds.toLongArray(),
                        groupedProductListType = GROUPED
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewLinkedProducts -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToLinkedProductsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductSelectionList -> {
                val action = ProductDetailFragmentDirections
                    .actionGlobalProductSelectionListFragment(
                        remoteProductId = target.remoteId,
                        groupedProductListType = target.groupedProductType,
                        excludedProductIds = target.excludedProductIds.toLongArray()
                    )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductAdd -> {
                val directions = NavGraphMainDirections.actionGlobalProductDetailFragment(
                    isAddProduct = true,
                    source = target.source
                )

                fragment.findNavController().navigateSafely(
                    directions = directions,
                    navOptions =
                    if (target.source == STORE_ONBOARDING)
                        NavOptions.Builder()
                            .setPopUpTo(R.id.dashboard, false)
                            .build()
                    else null
                )
            }

            is ViewProductDownloads -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductDownloadsFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductDownloadsSettings -> {
                val action = ProductDownloadsFragmentDirections
                    .actionProductDownloadsFragmentToProductDownloadsSettingsFragment()

                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductDownloadDetails -> {
                val action = NavGraphProductsDirections
                    .actionGlobalProductDownloadDetailsFragment(target.isEditing, target.file)
                fragment.findNavController().navigateSafely(action)
            }

            is ViewProductAddonsDetails -> {
                ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductAddonsFragment()
                    .apply { fragment.findNavController().navigateSafely(this) }
            }

            is AddProductDownloadableFile -> {
                val action = NavGraphProductsDirections.actionGlobalAddProductDownloadBottomSheetFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is AddProductAttribute -> {
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

            is RenameProductAttribute -> {
                val action = AddAttributeTermsFragmentDirections
                    .actionAttributeTermsFragmentToRenameAttributeFragment(target.attributeName)
                fragment.findNavController().navigateSafely(action)
            }

            is AddProductAttributeTerms -> {
                val action = NavGraphProductsDirections.actionGlobalAddVariationAttributeTermsFragment(
                    attributeId = target.attributeId,
                    attributeName = target.attributeName,
                    isNewAttribute = target.isNewAttribute,
                    isVariationCreation = target.isVariationCreation
                )
                fragment.findNavController().navigateSafely(action)
            }

            is ViewMediaUploadErrors -> {
                val action = NavGraphProductsDirections.actionGlobalMediaUploadErrorsFragment(target.remoteId)
                fragment.findNavController().navigateSafely(action)
            }

            is NavigateToVariationSelector -> {
                fragment.findNavController().navigateSafely(
                    ProductSelectorFragmentDirections.actionProductSelectorFragmentToVariationSelectorFragment(
                        target.productId,
                        target.selectedVariationIds.toLongArray(),
                        target.productSelectorFlow,
                        target.productSourceForTracking,
                    )
                )
            }

            is NavigateToProductFilter -> {
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

            is ProductNavigationTarget.ViewProductSubscription -> {
                val action = ProductDetailFragmentDirections.actionProductDetailFragmentToProductSubscriptionFragment(
                    target.subscription,
                    target.sale
                )
                fragment.findNavController().navigateSafely(action)
            }

            is ProductNavigationTarget.ViewProductQuantityRules -> {
                val action = ProductDetailFragmentDirections.actionProductDetailFragmentToProductQuantityRulesFragment(
                    target.quantityRules
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

            is ExitProduct -> fragment.findNavController().navigateUp()

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
