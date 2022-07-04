package com.woocommerce.android.ui.products

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_ATTRIBUTE_EDIT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewGroupedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewLinkedProducts
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAddonsDetails
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAttributes
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloads
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductExternalLink
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductReviews
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTypes
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.OTHER
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal

class ProductDetailCardBuilder(
    private val viewModel: ProductDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters,
    private val addonRepository: AddonRepository,
    private val variationRepository: VariationRepository
) {
    private lateinit var originalSku: String

    suspend fun buildPropertyCards(product: Product, originalSku: String): List<ProductPropertyCard> {
        this.originalSku = originalSku

        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(product))

        when (product.productType) {
            SIMPLE -> cards.addIfNotEmpty(getSimpleProductCard(product))
            VARIABLE -> cards.addIfNotEmpty(getVariableProductCard(product))
            GROUPED -> cards.addIfNotEmpty(getGroupedProductCard(product))
            EXTERNAL -> cards.addIfNotEmpty(getExternalProductCard(product))
            OTHER -> cards.addIfNotEmpty(getOtherProductCard(product))
        }

        return cards
    }

    private fun getPrimaryCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = PRIMARY,
            properties = listOf(
                product.title(),
                product.description()
            ).filterNotEmpty()
        )
    }

    private suspend fun getSimpleProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                product.productReviews(),
                product.inventory(SIMPLE),
                product.addons(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType(),
                product.downloads()
            ).filterNotEmpty()
        )
    }

    private suspend fun getGroupedProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.groupedProducts(),
                product.productReviews(),
                product.inventory(GROUPED),
                product.addons(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getExternalProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                product.productReviews(),
                product.externalLink(),
                product.inventory(EXTERNAL),
                product.addons(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getVariableProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.warning(),
                product.variations(),
                product.variationAttributes(),
                product.productReviews(),
                product.inventory(VARIABLE),
                product.addons(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    /**
     * Used for product types the app doesn't support yet (ex: subscriptions), uses a subset
     * of properties since we can't be sure pricing, shipping, etc., are applicable
     */
    private suspend fun getOtherProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.productReviews(),
                product.addons(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private fun Product.downloads(): ProductProperty? {
        if (!this.isDownloadable || this.downloads.isEmpty()) return null
        return ComplexProperty(
            title = string.product_downloadable_files,
            value = StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = this.downloads.size,
                default = string.product_downloadable_files_value_multiple,
                one = string.product_downloadable_files_value_single
            ),
            icon = drawable.ic_gridicons_cloud,
            onClick = {
                viewModel.onEditProductCardClicked(
                    ViewProductDownloads,
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_DOWNLOADABLE_FILES_TAPPED
                )
            }
        )
    }

    private fun Product.shortDescription(): ProductProperty? {
        return if (hasShortDescription) {
            ComplexProperty(
                string.product_short_description,
                shortDescription,
                drawable.ic_gridicons_align_left
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductShortDescriptionEditor(
                        shortDescription,
                        resources.getString(string.product_short_description)
                    ),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
                )
            }
        } else {
            null
        }
    }

    // show stock properties as a group if stock management is enabled and if the product type is [SIMPLE],
    // otherwise show sku separately
    private fun Product.inventory(productType: ProductType): ProductProperty {
        val inventory = mutableMapOf<String, String>()

        if (this.sku.isNotEmpty()) {
            inventory[resources.getString(string.product_sku)] = this.sku
        }

        if (productType == SIMPLE || productType == VARIABLE) {
            if (this.isStockManaged) {
                inventory[resources.getString(string.product_stock_quantity)] =
                    StringUtils.formatCountDecimal(this.stockQuantity)
                inventory[resources.getString(string.product_backorders)] =
                    ProductBackorderStatus.backordersToDisplayString(resources, this.backorderStatus)
            } else if (productType == SIMPLE) {
                inventory[resources.getString(string.product_stock_status)] =
                    ProductStockStatus.stockStatusToDisplayString(resources, this.stockStatus)
            }
        }

        if (inventory.isEmpty()) {
            inventory[""] = resources.getString(string.product_inventory_empty)
        }

        return PropertyGroup(
            string.product_inventory,
            inventory,
            drawable.ic_gridicons_list_checkmark,
            true
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductInventory(
                    InventoryData(
                        sku = this.sku,
                        isStockManaged = this.isStockManaged,
                        stockStatus = this.stockStatus,
                        stockQuantity = this.stockQuantity,
                        backorderStatus = this.backorderStatus,
                        isSoldIndividually = this.isSoldIndividually
                    ),
                    originalSku,
                    productType
                ),
                PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
            )
        }
    }

    private fun Product.shipping(): ProductProperty? {
        return if (!this.isVirtual && hasShipping) {
            val weightWithUnits = this.getWeightWithUnits(parameters.weightUnit)
            val sizeWithUnits = this.getSizeWithUnits(parameters.dimensionUnit)
            val shippingGroup = mapOf(
                Pair(resources.getString(string.product_weight), weightWithUnits),
                Pair(resources.getString(string.product_dimensions), sizeWithUnits),
                Pair(
                    resources.getString(string.product_shipping_class),
                    viewModel.getShippingClassByRemoteShippingClassId(this.shippingClassId)
                )
            )

            PropertyGroup(
                string.product_shipping,
                shippingGroup,
                drawable.ic_gridicons_shipping
            ) {
                viewModel.onEditProductCardClicked(
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
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
                )
            }
        } else {
            null
        }
    }

    // enable editing external product link
    private fun Product.externalLink(): ProductProperty? {
        return if (this.productType == EXTERNAL) {
            val hasExternalLink = this.externalUrl.isNotEmpty()
            val externalGroup = if (hasExternalLink) {
                mapOf(Pair("", this.externalUrl))
            } else {
                mapOf(Pair("", resources.getString(string.product_external_empty_link)))
            }

            PropertyGroup(
                string.product_external_link,
                externalGroup,
                drawable.ic_gridicons_link,
                hasExternalLink
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductExternalLink(this.remoteId),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_EXTERNAL_PRODUCT_LINK_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun Product.price(): ProductProperty {
        // If we have pricing info, show price & sales price as a group,
        // otherwise provide option to add pricing info for the product
        val pricingGroup = PriceUtils.getPriceGroup(
            parameters,
            resources,
            currencyFormatter,
            regularPrice,
            salePrice,
            isSaleScheduled,
            saleStartDateGmt,
            saleEndDateGmt
        )

        return PropertyGroup(
            string.product_price,
            pricingGroup,
            drawable.ic_gridicons_money,
            showTitle = this.regularPrice.isSet()
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductPricing(
                    PricingData(
                        taxClass,
                        taxStatus,
                        isSaleScheduled,
                        saleStartDateGmt,
                        saleEndDateGmt,
                        regularPrice,
                        salePrice
                    )
                ),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
            )
        }
    }

    private fun Product.productTypeDisplayName(): String {
        return when (productType) {
            SIMPLE -> {
                when {
                    this.isVirtual -> resources.getString(string.product_type_virtual)
                    this.isDownloadable -> resources.getString(string.product_type_downloadable)
                    else -> resources.getString(string.product_type_physical)
                }
            }
            VARIABLE -> resources.getString(string.product_type_variable)
            GROUPED -> resources.getString(string.product_type_grouped)
            EXTERNAL -> resources.getString(string.product_type_external)
            OTHER -> this.type.capitalize() // show the actual product type string for unsupported products
        }
    }

    private fun Product.productType(): ProductProperty {
        val onClickHandler = {
            viewModel.onEditProductCardClicked(
                ViewProductTypes(false, currentProductType = type, isCurrentProductVirtual = isVirtual),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_TYPE_TAPPED
            )
        }

        return ComplexProperty(
            string.product_type,
            resources.getString(string.product_detail_product_type_hint, productTypeDisplayName()),
            drawable.ic_gridicons_product,
            onClick = if (remoteId != 0L && productType != OTHER) onClickHandler else null
        )
    }

    private fun Product.productReviews(): ProductProperty? {
        return if (this.reviewsAllowed) {
            val value = when (this.ratingCount) {
                0 -> resources.getString(string.product_ratings_count_zero)
                1 -> resources.getString(string.product_ratings_count_one)
                else -> resources.getString(string.product_ratings_count, this.ratingCount)
            }
            RatingBar(
                string.product_reviews,
                value,
                this.averageRating,
                drawable.ic_reviews
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductReviews(this.remoteId),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_REVIEWS_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun Product.groupedProducts(): ProductProperty {
        val groupedProductsSize = this.groupedProductIds.size
        val showTitle = groupedProductsSize > 0

        val groupedProductsDesc = if (showTitle) {
            StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = groupedProductsSize,
                default = string.product_count_many,
                one = string.product_count_one,
            )
        } else {
            resources.getString(string.grouped_product_empty)
        }

        return ComplexProperty(
            string.grouped_products,
            groupedProductsDesc,
            drawable.ic_widgets,
            showTitle = showTitle
        ) {
            viewModel.onEditProductCardClicked(
                ViewGroupedProducts(this.remoteId, this.groupedProductIds),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_GROUPED_PRODUCTS_TAPPED
            )
        }
    }

    private fun Product.linkedProducts(): ProductProperty? {
        if (!hasLinkedProducts()) return null

        val upsellDesc = StringUtils.getQuantityString(
            resourceProvider = resources,
            quantity = this.upsellProductIds.size,
            one = string.upsell_product_count_one,
            default = string.upsell_product_count_many,
        )
        val crossSellDesc = StringUtils.getQuantityString(
            resourceProvider = resources,
            quantity = this.crossSellProductIds.size,
            one = string.cross_sell_product_count_one,
            default = string.cross_sell_product_count_many,
        )

        return ComplexProperty(
            string.product_detail_linked_products,
            "$upsellDesc<br>$crossSellDesc",
            drawable.ic_gridicons_reblog,
            maxLines = 2
        ) {
            viewModel.onEditProductCardClicked(
                ViewLinkedProducts(this.remoteId),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_LINKED_PRODUCTS_TAPPED
            )
        }
    }

    private fun Product.title(): ProductProperty {
        val name = this.name.fastStripHtml()
        val (badgeText, badgeColor) = this.status.getBadgeResources()
        return Editable(
            hint = string.product_detail_title_hint,
            text = name,
            badgeText = badgeText,
            badgeColor = badgeColor,
            onTextChanged = viewModel::onProductTitleChanged
        )
    }

    private fun Product.description(): ProductProperty {
        val productDescription = this.description
        val productTitle = this.name
        val showTitle = productDescription.isNotEmpty()
        val description = if (productDescription.isEmpty()) {
            resources.getString(string.product_description_empty)
        } else {
            productDescription
        }

        return ComplexProperty(
            string.product_description,
            description,
            showTitle = showTitle
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductDescriptionEditor(
                    productDescription, resources.getString(string.product_description),
                    productTitle
                ),
                PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
            )
        }
    }

    // show product variations only if product type is variable and if there are variations for the product
    private fun Product.variations(): ProductProperty {
        return if (this.numVariations > 0 && this.variationEnabledAttributes.isNotEmpty()) {
            val content = StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = numVariations,
                default = string.product_variation_multiple_count,
                one = string.product_variation_single_count
            )

            ComplexProperty(
                string.product_variations,
                content,
                drawable.ic_gridicons_types
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductVariations(this.remoteId),
                    PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED
                )
            }
        } else {
            emptyVariations()
        }
    }

    private fun Product.emptyVariations() =
        ComplexProperty(
            value = resources.getString(string.product_detail_add_variations),
            icon = drawable.ic_gridicons_types,
            showTitle = false,
            onClick = {
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCT_VARIATION_ADD_FIRST_TAPPED,
                    mapOf(AnalyticsTracker.KEY_PRODUCT_ID to remoteId)
                )
                viewModel.saveAsDraftIfNewVariableProduct()
                viewModel.onAddFirstVariationClicked()
            }
        )

    private fun Product.variationAttributes() =
        takeIf { this.variationEnabledAttributes.isNotEmpty() }?.let {
            val properties = mutableMapOf<String, String>()
            for (attribute in this.variationEnabledAttributes) {
                properties[attribute.name] = attribute.terms.size.toString()
            }

            PropertyGroup(
                string.variable_product_attributes,
                properties,
                drawable.ic_gridicons_customize,
                propertyFormat = string.product_variation_options
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductAttributes,
                    PRODUCT_ATTRIBUTE_EDIT_BUTTON_TAPPED
                )
            }
        }

    private fun Product.categories(): ProductProperty? {
        return if (hasCategories) {
            val categories = categories.joinToString(transform = { it.name })

            ComplexProperty(
                string.product_categories,
                categories,
                drawable.ic_gridicons_folder,
                maxLines = 5
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductCategories(this.remoteId),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun Product.tags(): ProductProperty? {
        return if (hasTags) {
            val tags = this.tags.joinToString(transform = { it.name })

            ComplexProperty(
                string.product_tags,
                tags,
                drawable.ic_gridicons_tag,
                maxLines = 5
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductTags(this.remoteId),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_TAGS_TAPPED
                )
            }
        } else {
            null
        }
    }

    private suspend fun Product.addons(): ProductProperty? =
        takeIf { product ->
            addonRepository.hasAnyProductSpecificAddons(
                productRemoteID = product.remoteId
            ) && AppPrefs.isProductAddonsEnabled
        }?.let {
            ComplexProperty(
                value = resources.getString(string.product_add_ons_title),
                icon = drawable.ic_gridicon_circle_plus,
                showTitle = false,
                onClick = {
                    viewModel.onEditProductCardClicked(
                        ViewProductAddonsDetails,
                        AnalyticsEvent.PRODUCT_ADDONS_PRODUCT_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED
                    )
                }
            )
        }

    private fun Product.warning(): ProductProperty? {
        val variations = variationRepository.getProductVariationList(this.remoteId)

        val missingPriceVariation = variations
            .find { it.regularPrice == null || it.regularPrice == BigDecimal.ZERO }

        return missingPriceVariation?.let {
            ProductProperty.Warning(resources.getString(string.variation_detail_price_warning))
        }
    }
}

fun ProductStatus?.getBadgeResources(): Pair<Int?, Int?> {
    return if (this == null) Pair(null, null)
    else when (this) {
        ProductStatus.PUBLISH -> Pair(null, null)
        ProductStatus.PENDING -> Pair(string.product_status_pending, R.color.product_status_badge_pending)
        ProductStatus.PRIVATE -> Pair(string.product_status_privately_published, R.color.product_status_badge_draft)
        else -> Pair(this.stringResource, R.color.product_status_badge_draft)
    }
}
