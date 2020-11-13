package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewLinkedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductDownloadableFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.OTHER
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductDetailBottomSheetBuilder(
    private val resources: ResourceProvider
) {
    enum class ProductDetailBottomSheetType(
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int
    ) {
        PRODUCT_SHIPPING(string.product_shipping, string.bottom_sheet_shipping_desc),
        PRODUCT_CATEGORIES(string.product_categories, string.bottom_sheet_categories_desc),
        PRODUCT_TAGS(string.product_tags, string.bottom_sheet_tags_desc),
        SHORT_DESCRIPTION(string.product_short_description, string.bottom_sheet_short_description_desc),
        LINKED_PRODUCTS(string.product_detail_linked_products, string.bottom_sheet_linked_products_desc),
        PRODUCT_DOWNLOADS(R.string.product_downloadable_files, string.bottom_sheet_downloadable_files_desc)
    }

    data class ProductDetailBottomSheetUiItem(
        val type: ProductDetailBottomSheetType,
        val clickEvent: ProductNavigationTarget,
        val stat: Stat? = null
    )

    fun buildBottomSheetList(product: Product): List<ProductDetailBottomSheetUiItem> {
        return when (product.productType) {
            SIMPLE -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription(),
                    product.getLinkedProducts(),
                    product.getDownloadableFiles()
                )
            }
            EXTERNAL -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription(),
                    product.getLinkedProducts()
                )
            }
            GROUPED -> {
                listOfNotNull(
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription(),
                    product.getLinkedProducts()
                )
            }
            VARIABLE -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription(),
                    product.getLinkedProducts()
                )
            }
            OTHER -> {
                listOfNotNull(
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription()
                )
            }
        }
    }

    private fun Product.getShipping(): ProductDetailBottomSheetUiItem? {
        return if (!isVirtual && !hasShipping) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_SHIPPING,
                ViewProductShipping(
                    ShippingData(
                        weight,
                        length,
                        width,
                        height,
                        shippingClass,
                        shippingClassId
                    )
                ),
                Stat.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getCategories(): ProductDetailBottomSheetUiItem? {
        return if (!hasCategories) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_CATEGORIES,
                ViewProductCategories(remoteId),
                Stat.PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getTags(): ProductDetailBottomSheetUiItem? {
        return if (!hasTags) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_TAGS,
                ViewProductTags(remoteId)
            )
        } else {
            null
        }
    }

    private fun Product.getShortDescription(): ProductDetailBottomSheetUiItem? {
        return if (!hasShortDescription) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.SHORT_DESCRIPTION,
                ViewProductShortDescriptionEditor(
                    shortDescription,
                    resources.getString(string.product_short_description)
                ),
                Stat.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getLinkedProducts(): ProductDetailBottomSheetUiItem? {
        return if (!hasLinkedProducts() && FeatureFlag.PRODUCT_RELEASE_M5.isEnabled()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.LINKED_PRODUCTS,
                ViewLinkedProducts(remoteId),
                Stat.PRODUCT_DETAIL_VIEW_LINKED_PRODUCTS_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getDownloadableFiles(): ProductDetailBottomSheetUiItem? {
        if (!FeatureFlag.PRODUCT_RELEASE_M5.isEnabled() || (isDownloadable && downloads.isNotEmpty())) return null
        return ProductDetailBottomSheetUiItem(
            ProductDetailBottomSheetType.PRODUCT_DOWNLOADS,
            AddProductDownloadableFile
        )
    }
}
