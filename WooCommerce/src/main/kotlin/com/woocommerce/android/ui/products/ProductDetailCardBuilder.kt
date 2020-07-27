package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductExternalLink
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.ReadMore
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRICING
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PURCHASE_DETAILS
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.FormatUtils

class ProductDetailCardBuilder(
    private val viewModel: ProductDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters
) {
    private fun isSimple(product: Product) = product.type == SIMPLE

    fun buildPropertyCards(product: Product): List<ProductPropertyCard> {
        val cards = mutableListOf<ProductPropertyCard>()

        cards.addIfNotEmpty(getPrimaryCard(product))

        if (FeatureFlag.PRODUCT_RELEASE_M3.isEnabled()) {
            when (product.type) {
                SIMPLE -> cards.addIfNotEmpty(getSimpleProductCard(product))
                VARIABLE -> cards.addIfNotEmpty(getVariableProductCard(product))
                GROUPED -> cards.addIfNotEmpty(getGroupedProductCard(product))
                EXTERNAL -> cards.addIfNotEmpty(getExternalProductCard(product))
            }
        } else {
            if (isSimple(product)) {
                cards.addIfNotEmpty(getSimpleProductCard(product))
            } else {
                cards.addIfNotEmpty(getPricingAndInventoryCard(product))
            }
        }

        cards.addIfNotEmpty(getPurchaseDetailsCard(product))

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

    private fun getSimpleProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                product.productType(),
                product.shipping(),
                product.inventory(),
                product.shortDescription(),
                product.categories(),
                product.tags()
            ).filterNotEmpty()
        )
    }

    private fun getGroupedProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.productType(),
                product.shortDescription(),
                product.inventory(),
                product.categories(),
                product.tags()
            ).filterNotEmpty()
        )
    }

    private fun getExternalProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                product.productType(),
                product.externalLink(),
                product.shortDescription(),
                product.inventory(),
                product.categories(),
                product.tags()
            ).filterNotEmpty()
        )
    }

    private fun getVariableProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.productType(),
                product.variations(),
                product.shortDescription(),
                product.categories(),
                product.tags()
            ).filterNotEmpty()
        )
    }

    /**
     * Existing product detail card UI which that will be replaced by the new design once
     * Product Release 1 changes are completed.
     */
    private fun getPricingAndInventoryCard(product: Product): ProductPropertyCard {
        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.regularPrice != null || product.salePrice != null
        val cardTitle = if (hasPricingInfo) {
            resources.getString(R.string.product_pricing_and_inventory)
        } else {
            resources.getString(R.string.product_inventory)
        }

        return ProductPropertyCard(
            PRICING,
            cardTitle,
            listOf(
                product.variations(),
                product.readOnlyPrice(),
                product.readOnlyInventory()
            ).filterNotEmpty()
        )
    }

    private fun getPurchaseDetailsCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            PURCHASE_DETAILS,
            resources.getString(R.string.product_purchase_details),
            listOf(
                product.readOnlyShipping(),
                product.downloads(),
                product.purchaseNote()
            ).filterNotEmpty()
        )
    }

    // if add/edit products is enabled, purchase note appears in product settings
    private fun Product.purchaseNote(): ProductProperty? {
        return if (this.purchaseNote.isNotBlank() && !isSimple(this)) {
            ReadMore(
                R.string.product_purchase_note,
                this.purchaseNote
            )
        } else {
            null
        }
    }

    private fun Product.downloads(): ProductProperty? {
        return if (this.isDownloadable) {
            val limit = if (this.downloadLimit > 0) String.format(
                resources.getString(R.string.product_download_limit_count),
                this.downloadLimit
            ) else ""
            val expiry = if (this.downloadExpiry > 0) String.format(
                resources.getString(R.string.product_download_expiry_days),
                this.downloadExpiry
            ) else ""

            val downloadGroup = mapOf(
                Pair(resources.getString(R.string.product_downloadable_files), this.fileCount.toString()),
                Pair(resources.getString(R.string.product_download_limit), limit),
                Pair(resources.getString(R.string.product_download_expiry), expiry)
            )
            PropertyGroup(
                R.string.product_downloads,
                downloadGroup
            )
        } else {
            null
        }
    }

    // shipping group is part of the secondary card if edit product is enabled
    private fun Product.readOnlyShipping(): ProductProperty? {
        return if (!isSimple(this)) {
            val shippingGroup = mapOf(
                Pair(resources.getString(R.string.product_weight), this.getWeightWithUnits(parameters.weightUnit)),
                Pair(resources.getString(R.string.product_size), this.getSizeWithUnits(parameters.dimensionUnit)),
                Pair(resources.getString(R.string.product_shipping_class), this.shippingClass)
            )
            PropertyGroup(
                R.string.product_shipping,
                shippingGroup
            )
        } else {
            null
        }
    }

    // show stock properties as a group if stock management is enabled, otherwise show sku separately
    private fun Product.readOnlyInventory(): ProductProperty {
        return when {
            this.manageStock -> {
                val group = mapOf(
                    Pair(resources.getString(R.string.product_stock_status),
                        ProductStockStatus.stockStatusToDisplayString(resources, this.stockStatus)
                    ),
                    Pair(resources.getString(R.string.product_backorders),
                        ProductBackorderStatus.backordersToDisplayString(resources, this.backorderStatus)
                    ),
                    Pair(resources.getString(R.string.product_stock_quantity),
                        StringUtils.formatCount(this.stockQuantity)
                    ),
                    Pair(resources.getString(R.string.product_sku), this.sku)
                )
                PropertyGroup(
                    R.string.product_inventory,
                    group
                )
            }
            FeatureFlag.PRODUCT_RELEASE_M3.isEnabled() -> {
                ComplexProperty(
                    R.string.product_sku,
                    this.sku,
                    R.drawable.ic_gridicons_list_checkmark
                )
            }
            else -> {
                ComplexProperty(
                    R.string.product_sku,
                    this.sku
                )
            }
        }
    }

    private fun Product.readOnlyPrice(): ProductProperty? {
        val hasPricingInfo = this.regularPrice != null || this.salePrice != null
        return if (hasPricingInfo) {
            // when there's a sale price show price & sales price as a group, otherwise show price separately
            return if (this.isOnSale) {
                val group = mapOf(
                    resources.getString(R.string.product_regular_price)
                        to PriceUtils.formatCurrency(this.regularPrice, parameters.currencyCode, currencyFormatter),
                    resources.getString(R.string.product_sale_price)
                        to PriceUtils.formatCurrency(this.salePrice, parameters.currencyCode, currencyFormatter)
                )
                PropertyGroup(R.string.product_price, group)
            } else {
                ComplexProperty(
                    R.string.product_price,
                    PriceUtils.formatCurrency(this.regularPrice, parameters.currencyCode, currencyFormatter)
                )
            }
        } else {
            null
        }
    }

    private fun Product.shortDescription(): ProductProperty? {
        return if (hasShortDescription) {
            ComplexProperty(
                R.string.product_short_description,
                shortDescription,
                R.drawable.ic_gridicons_align_left
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductShortDescriptionEditor(
                        shortDescription,
                        resources.getString(R.string.product_short_description)
                    ),
                    Stat.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
                )
            }
        } else {
            null
        }
    }

    // show stock properties as a group if stock management is enabled and if the product type is [SIMPLE],
    // otherwise show sku separately
    private fun Product.inventory(): ProductProperty {
        val inventoryGroup = when {
            this.type == GROUPED || this.type == EXTERNAL -> {
                mapOf(
                    Pair(resources.getString(R.string.product_sku), this.sku)
                )
            }
            this.manageStock -> mapOf(
                Pair(resources.getString(R.string.product_backorders),
                    ProductBackorderStatus.backordersToDisplayString(resources, this.backorderStatus)),
                Pair(resources.getString(R.string.product_stock_quantity),
                    FormatUtils.formatInt(this.stockQuantity)),
                Pair(resources.getString(R.string.product_sku), this.sku)
            )
            this.sku.isNotEmpty() -> mapOf(
                Pair(resources.getString(R.string.product_sku), this.sku),
                Pair(resources.getString(R.string.product_stock_status),
                    ProductStockStatus.stockStatusToDisplayString(resources, this.stockStatus))
            )
            else -> mapOf(
                Pair("", ProductStockStatus.stockStatusToDisplayString(resources, this.stockStatus))
            )
        }

        return PropertyGroup(
            R.string.product_inventory,
            inventoryGroup,
            R.drawable.ic_gridicons_list_checkmark,
            true
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductInventory(this.remoteId),
                Stat.PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
            )
        }
    }

    private fun Product.shipping(): ProductProperty? {
        return if (!this.isVirtual && hasShipping) {
            val weightWithUnits = this.getWeightWithUnits(parameters.weightUnit)
            val sizeWithUnits = this.getSizeWithUnits(parameters.dimensionUnit)
            val shippingGroup = mapOf(
                Pair(resources.getString(R.string.product_weight), weightWithUnits),
                Pair(resources.getString(R.string.product_dimensions), sizeWithUnits),
                Pair(
                    resources.getString(R.string.product_shipping_class),
                    viewModel.getShippingClassByRemoteShippingClassId(this.shippingClassId)
                )
            )

            PropertyGroup(
                R.string.product_shipping,
                shippingGroup,
                R.drawable.ic_gridicons_shipping
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductShipping(this.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
                )
            }
        } else {
            null
        }
    }

    // enable editing external product link
    private fun Product.externalLink(): ProductProperty? {
        return if (this.type == EXTERNAL) {
            val hasExternalLink = this.externalUrl.isNotEmpty()
            val externalGroup = if (hasExternalLink) {
                mapOf(Pair("", this.externalUrl))
            } else {
                mapOf(Pair("", resources.getString(R.string.product_external_empty_link)))
            }

            PropertyGroup(
                R.string.product_external_link,
                externalGroup,
                R.drawable.ic_gridicons_link,
                hasExternalLink
            ) {
                viewModel.onEditProductCardClicked(ViewProductExternalLink(this.remoteId))
            }
        } else {
            null
        }
    }

    private fun Product.price(): ProductProperty {
        // If we have pricing info, show price & sales price as a group,
        // otherwise provide option to add pricing info for the product
        val hasPricingInfo = this.regularPrice != null || this.salePrice != null
        val pricingGroup = PriceUtils.getPriceGroup(
            parameters,
            resources,
            currencyFormatter,
            regularPrice,
            salePrice,
            isSaleScheduled,
            isOnSale,
            saleStartDateGmt,
            saleEndDateGmt
        )

        return PropertyGroup(
            R.string.product_price,
            pricingGroup,
            R.drawable.ic_gridicons_money,
            hasPricingInfo
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductPricing(this.remoteId),
                Stat.PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
            )
        }
    }

    private fun Product.productType(): ProductProperty? {
        return if (FeatureFlag.PRODUCT_RELEASE_M3.isEnabled()) {
            val productType = resources.getString(this.getProductTypeFormattedForDisplay())
            ComplexProperty(
                R.string.product_type,
                resources.getString(R.string.product_detail_product_type_hint, productType),
                R.drawable.ic_gridicons_product
            )
        } else {
            null
        }
    }

    private fun Product.title(): ProductProperty {
        val name = this.name.fastStripHtml()
        return if (isSimple(this) || FeatureFlag.PRODUCT_RELEASE_M3.isEnabled()) {
            Editable(
                R.string.product_detail_title_hint,
                name,
                onTextChanged = viewModel::onProductTitleChanged
            )
        } else {
            ComplexProperty(R.string.product_name, name)
        }
    }

    private fun Product.description(): ProductProperty? {
        val productDescription = this.description
        val showTitle = productDescription.isNotEmpty()
        val description = if (productDescription.isEmpty()) {
            resources.getString(R.string.product_description_empty)
        } else {
            productDescription
        }

        return when {
            isSimple(this) || FeatureFlag.PRODUCT_RELEASE_M3.isEnabled() -> {
                ComplexProperty(
                    R.string.product_description,
                    description,
                    showTitle = showTitle
                ) {
                    viewModel.onEditProductCardClicked(
                        ViewProductDescriptionEditor(
                            productDescription, resources.getString(R.string.product_description)
                        ),
                        PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
                    )
                }
            }
            productDescription.isNotEmpty() -> {
                ComplexProperty(R.string.product_description, description)
            }
            else -> {
                null
            }
        }
    }

    // show product variants only if product type is variable and if there are variations for the product
    private fun Product.variations(): ProductProperty? {
        return if (this.numVariations > 0) {
            val properties = mutableMapOf<String, String>()
            for (attribute in this.attributes) {
                properties[attribute.name] = attribute.options.size.toString()
            }

            PropertyGroup(
                R.string.product_variations,
                properties,
                R.drawable.ic_gridicons_types,
                propertyFormat = R.string.product_variation_options
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductVariations(this.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun Product.categories(): ProductProperty? {
        return if (FeatureFlag.PRODUCT_RELEASE_M3.isEnabled() && hasCategories) {
            val categories = categories.joinToString(transform = { it.name })

            ComplexProperty(
                R.string.product_categories,
                categories,
                R.drawable.ic_gridicons_folder,
                maxLines = 5
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductCategories(this.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun Product.tags(): ProductProperty? {
        return if (FeatureFlag.PRODUCT_RELEASE_M3.isEnabled() && hasTags) {
            val tags = this.tags.joinToString(transform = { it.name })

            ComplexProperty(
                R.string.product_tags,
                tags,
                R.drawable.ic_gridicons_tag,
                maxLines = 5
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductTags(this.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_TAGS_TAPPED
                )
            }
        } else {
            null
        }
    }
}
