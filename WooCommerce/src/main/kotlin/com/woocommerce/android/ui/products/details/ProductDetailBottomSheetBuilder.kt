package com.woocommerce.android.ui.products.details

import androidx.annotation.StringRes
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAggregate
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductDownloadableFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewLinkedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.SUBSCRIPTION
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE_SUBSCRIPTION
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductDetailBottomSheetBuilder(
    private val resources: ResourceProvider,
    private val variationRepository: VariationRepository,
    private val customFieldsRepository: CustomFieldsRepository
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
        PRODUCT_DOWNLOADS(string.product_downloadable_files, string.bottom_sheet_downloadable_files_desc),
        CUSTOM_FIELDS(string.product_custom_fields, string.product_custom_fields_desc)
    }

    data class ProductDetailBottomSheetUiItem(
        val type: ProductDetailBottomSheetType,
        val clickEvent: ProductNavigationTarget,
        val stat: AnalyticsEvent? = null
    )

    @Suppress("LongMethod")
    suspend fun buildBottomSheetList(productAggregate: ProductAggregate): List<ProductDetailBottomSheetUiItem> {
        return when (productAggregate.product.productType) {
            SIMPLE, SUBSCRIPTION -> {
                listOfNotNull(
                    productAggregate.getShipping(),
                    productAggregate.product.getCategories(),
                    productAggregate.product.getTags(),
                    productAggregate.product.getShortDescription(),
                    productAggregate.product.getLinkedProducts(),
                    productAggregate.product.getDownloadableFiles(),
                    productAggregate.product.getCustomFields()
                )
            }

            EXTERNAL -> {
                listOfNotNull(
                    productAggregate.product.getCategories(),
                    productAggregate.product.getTags(),
                    productAggregate.product.getShortDescription(),
                    productAggregate.product.getLinkedProducts(),
                    productAggregate.product.getCustomFields()
                )
            }

            GROUPED -> {
                listOfNotNull(
                    productAggregate.product.getCategories(),
                    productAggregate.product.getTags(),
                    productAggregate.product.getShortDescription(),
                    productAggregate.product.getLinkedProducts(),
                    productAggregate.product.getCustomFields()
                )
            }

            VARIABLE, VARIABLE_SUBSCRIPTION -> {
                listOfNotNull(
                    productAggregate.getShipping(),
                    productAggregate.product.getCategories(),
                    productAggregate.product.getTags(),
                    productAggregate.product.getShortDescription(),
                    productAggregate.product.getLinkedProducts(),
                    productAggregate.product.getCustomFields()
                )
            }

            else -> {
                listOfNotNull(
                    productAggregate.product.getCategories(),
                    productAggregate.product.getTags(),
                    productAggregate.product.getShortDescription(),
                    productAggregate.product.getCustomFields()
                )
            }
        }
    }

    private fun ProductAggregate.getShipping(): ProductDetailBottomSheetUiItem? {
        return if (!product.isVirtual && !hasShipping) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_SHIPPING,
                ViewProductShipping(
                    ShippingData(
                        weight = product.weight,
                        length = product.length,
                        width = product.width,
                        height = product.height,
                        shippingClassSlug = product.shippingClass,
                        shippingClassId = product.shippingClassId,
                        subscriptionShippingData = if (product.productType == SUBSCRIPTION ||
                            product.productType == VARIABLE_SUBSCRIPTION
                        ) {
                            ShippingData.SubscriptionShippingData(
                                oneTimeShipping = subscription?.oneTimeShipping ?: false,
                                canEnableOneTimeShipping = if (product.productType == SUBSCRIPTION) {
                                    subscription?.supportsOneTimeShipping ?: false
                                } else {
                                    // For variable subscription products, we need to check against the variations
                                    variationRepository.getProductVariationList(product.remoteId).all {
                                        (it as? SubscriptionProductVariation)?.subscriptionDetails
                                            ?.supportsOneTimeShipping ?: false
                                    }
                                }
                            )
                        } else {
                            null
                        }
                    )
                ),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
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
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED
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
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getLinkedProducts(): ProductDetailBottomSheetUiItem? {
        return if (!hasLinkedProducts()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.LINKED_PRODUCTS,
                ViewLinkedProducts(remoteId),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_LINKED_PRODUCTS_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getDownloadableFiles(): ProductDetailBottomSheetUiItem? {
        if (isDownloadable && downloads.isNotEmpty()) return null
        return ProductDetailBottomSheetUiItem(
            ProductDetailBottomSheetType.PRODUCT_DOWNLOADS,
            AddProductDownloadableFile
        )
    }

    private suspend fun Product.getCustomFields(): ProductDetailBottomSheetUiItem? {
        if (!FeatureFlag.CUSTOM_FIELDS.isEnabled() ||
            remoteId == ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID ||
            customFieldsRepository.hasDisplayableCustomFields(remoteId)
        ) {
            return null
        }

        return ProductDetailBottomSheetUiItem(
            ProductDetailBottomSheetType.CUSTOM_FIELDS,
            ProductNavigationTarget.ViewCustomFields(remoteId),
            AnalyticsEvent.PRODUCT_DETAIL_CUSTOM_FIELDS_TAPPED
        )
    }
}
