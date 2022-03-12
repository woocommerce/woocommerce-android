package com.woocommerce.android.ui.products

import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.*
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.GroupedProductListType.GROUPED
import com.woocommerce.android.ui.products.ProductNavigationTarget.*
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragmentDirections
import com.woocommerce.android.ui.products.downloads.ProductDownloadsFragmentDirections
import com.woocommerce.android.ui.products.settings.ProductSettingsFragmentDirections
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsFragmentDirections
import com.woocommerce.android.ui.products.variations.attributes.AttributeListFragmentDirections
import com.woocommerce.android.util.WooLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    companion object {
        const val IMAGE_PNG_QUALITY = 80
        const val SHARE_IMAGE_NAME = "image.png"
        const val SHARE_IMAGE_FOLDER = "images"
    }

    fun navigate(fragment: Fragment, target: ProductNavigationTarget) {
        when (target) {
            is ShareProduct -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToProductDetailShareOptionBottomSheetFragment()
                fragment.findNavController().navigateSafely(action)
            }

            is ShareProductPage -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, target.title)
                    putExtra(Intent.EXTRA_TEXT, target.url)
                    type = "text/plain"
                }
                val title = fragment.resources.getText(R.string.product_share_dialog_title)
                fragment.startActivity(Intent.createChooser(shareIntent, title))
            }

            is ShareProductImage -> {
                val cachePath = File(fragment.requireContext().cacheDir, SHARE_IMAGE_FOLDER)

                try {
                    cachePath.mkdir()
                    val stream = FileOutputStream("$cachePath/$SHARE_IMAGE_NAME")
                    target.image.compress(Bitmap.CompressFormat.PNG, IMAGE_PNG_QUALITY, stream)
                    stream.close()
                } catch (ex: IOException) {
                    WooLog.e(WooLog.T.UTILS, ex)
                }

                val newImageFile = File(cachePath, SHARE_IMAGE_NAME)
                val authority = BuildConfig.APPLICATION_ID + ".provider"
                val uriToImage = FileProvider.getUriForFile(fragment.requireContext(), authority, newImageFile)
                if (uriToImage != null) {
                    val intent = Intent()
                    intent.action = Intent.ACTION_SEND
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_STREAM, uriToImage)
                    val chooseTitle = fragment.resources.getText(R.string.product_share_dialog_choose_app)
                    fragment.startActivity(Intent.createChooser(intent, chooseTitle))
                }
            }

            is ViewProductVariations -> {
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToVariationListFragment(
                        target.remoteId
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
                    .actionProductSettingsFragmentToProductVisibilityFragment(visibility, target.password)
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
                val action = NavGraphMainDirections.actionGlobalProductDetailFragment(isAddProduct = true)
                fragment.findNavController().navigateSafely(action)
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

            is ExitProduct -> fragment.findNavController().navigateUp()
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
