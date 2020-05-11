package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.addPropertyIfNotEmpty
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductDetailViewModel.Parameters
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductInventory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.Link
import com.woocommerce.android.ui.products.models.ProductProperty.Property
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.models.ProductProperty.ReadMore
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRICING
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PURCHASE_DETAILS
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

class ProductDetailCardBuilder(
    private val viewModel: ProductDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: Parameters
) {
    /**
     * Add/Edit Product Release 1 is enabled by default for SIMPLE products
     */
    private fun isAddEditProductRelease1Enabled(productType: ProductType) = productType == ProductType.SIMPLE

    fun buildPropertyCards(product: Product): List<ProductPropertyCard> {
        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(product))

        // display pricing/inventory card only if product is not a variable product
        // since pricing, inventory, shipping and SKU for a variable product can differ per variant
        if (product.type != VARIABLE) {
            if (isAddEditProductRelease1Enabled(product.type)) {
                cards.addIfNotEmpty(getSecondaryCard(product))
            } else {
                cards.addIfNotEmpty(getPricingAndInventoryCard(product))
            }
        }
        cards.addIfNotEmpty(getPurchaseDetailsCard(product))

        return cards
    }

    private fun getPrimaryCard(product: Product): ProductPropertyCard {
        val items = mutableListOf<ProductProperty>()

        val productName = product.name.fastStripHtml()

        if (isAddEditProductRelease1Enabled(product.type)) {
            items.add(Editable(R.string.product_detail_title_hint, productName, onTextChanged = viewModel::onProductTitleChanged))
        } else {
            items.addPropertyIfNotEmpty(ComplexProperty(R.string.product_name, productName))
        }

        if (isAddEditProductRelease1Enabled(product.type)) {
            val productDescription = product.description
            val showTitle = productDescription.isNotEmpty()
            val description = if (productDescription.isEmpty()) {
                resources.getString(R.string.product_description_empty)
            } else {
                productDescription
            }
            items.addPropertyIfNotEmpty(
                ComplexProperty(
                    R.string.product_description,
                    description,
                    showTitle = showTitle
                ) {
                    viewModel.onEditProductCardClicked(
                        ViewProductDescriptionEditor(
                            productDescription, resources.getString(R.string.product_description)
                        ),
                        Stat.PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
                    )
                }
            )
        }

        // we don't show total sales for variations because they're always zero
        // we are removing the total orders sections from products M2 release
        if (product.type != VARIABLE && !FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()) {
            items.addPropertyIfNotEmpty(Property(
                R.string.product_total_orders,
                StringUtils.formatCount(product.totalSales)
            ))
        }

        // we don't show reviews for variations because they're always empty
        if (product.type != VARIABLE && product.reviewsAllowed) {
            items.add(
                RatingBar(
                    R.string.product_reviews,
                    StringUtils.formatCount(product.ratingCount),
                    product.averageRating
                )
            )
        }

        // show product variants only if product type is variable and if there are variations for the product
        if (product.type == VARIABLE && product.numVariations > 0) {
            val properties = mutableMapOf<String, String>()
            for (attribute in product.attributes) {
                properties[attribute.name] = attribute.options.size.toString()
            }

            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_variations,
                    properties
                ) {
                    viewModel.onEditProductCardClicked(
                        ViewProductVariations(product.remoteId),
                        Stat.PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED
                    )
                }
            )
        }

        if (!FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()) {
            // display `View product on Store` (in M2 this is in the options menu)
            items.add(
                Link(R.string.product_view_in_store) {
                    viewModel.onViewProductOnStoreLinkClicked(product.permalink)
                }
            )

            // enable viewing affiliate link for external products (in M2 this is editable)
            if (product.type == ProductType.EXTERNAL) {
                items.add(
                    Link(R.string.product_view_affiliate) {
                        viewModel.onAffiliateLinkClicked(product.externalUrl)
                    }
                )
            }
        }

        return ProductPropertyCard(type = PRIMARY, properties = items)
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    private fun getProductSaleDates(dateOnSaleFrom: Date, dateOnSaleTo: Date): String {
        val formattedFromDate = if (DateTimeUtils.isSameYear(dateOnSaleFrom, dateOnSaleTo)) {
            dateOnSaleFrom.formatToMMMdd()
        } else {
            dateOnSaleFrom.formatToMMMddYYYY()
        }
        return resources.getString(
            R.string.product_sale_date_from_to,
            formattedFromDate,
            dateOnSaleTo.formatToMMMddYYYY()
        )
    }

    /**
     * New product detail card UI slated for new products release 1
     */
    private fun getSecondaryCard(product: Product): ProductPropertyCard {
        val items = mutableListOf<ProductProperty>()

        // If we have pricing info, show price & sales price as a group,
        // otherwise provide option to add pricing info for the product
        val hasPricingInfo = product.regularPrice != null || product.salePrice != null
        val pricingGroup = mutableMapOf<String, String>()
        if (hasPricingInfo) {
            // regular product price
            pricingGroup[resources.getString(R.string.product_regular_price)] = formatCurrency(
                product.regularPrice,
                parameters.currencyCode
            )
            // display product sale price if it's on sale
            if (product.isOnSale) {
                pricingGroup[resources.getString(R.string.product_sale_price)] = formatCurrency(
                    product.salePrice,
                    parameters.currencyCode
                )
            }

            // display product sale dates using the site's timezone, if available
            if (product.isSaleScheduled) {
                var dateOnSaleFrom = product.saleStartDateGmt?.let {
                    DateUtils.offsetGmtDate(it, parameters.gmtOffset)
                }
                val dateOnSaleTo = product.saleEndDateGmt?.let {
                    DateUtils.offsetGmtDate(it, parameters.gmtOffset)
                }
                if (dateOnSaleTo != null && dateOnSaleFrom == null) {
                    dateOnSaleFrom = DateUtils.offsetGmtDate(Date(), parameters.gmtOffset)
                }
                val saleDates = when {
                    (dateOnSaleFrom != null && dateOnSaleTo != null) -> {
                        getProductSaleDates(dateOnSaleFrom, dateOnSaleTo)
                    }
                    (dateOnSaleFrom != null && dateOnSaleTo == null) -> {
                        resources.getString(R.string.product_sale_date_from, dateOnSaleFrom.formatToMMMddYYYY())
                    }
                    else -> null
                }
                saleDates?.let {
                    pricingGroup[resources.getString(R.string.product_sale_dates)] = it
                }
            }
        } else {
            pricingGroup[""] = resources.getString(R.string.product_price_empty)
        }

        items.addPropertyIfNotEmpty(
            PropertyGroup(
                R.string.product_price,
                pricingGroup,
                R.drawable.ic_gridicons_money,
                hasPricingInfo
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductPricing(product.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
                )
            }
        )

        // enable editing external product link
        if (FeatureFlag.PRODUCT_RELEASE_M2.isEnabled() && product.type == ProductType.EXTERNAL) {
            val hasExternalLink = product.externalUrl.isNotEmpty()
            val externalGroup = if (hasExternalLink) {
                mapOf(Pair("", product.externalUrl))
            } else {
                mapOf(Pair("", resources.getString(R.string.product_external_empty_link)))
            }

            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_external_link,
                    externalGroup,
                    R.drawable.ic_gridicons_link,
                    hasExternalLink
                ) {
                    viewModel.onEditProductCardClicked(ViewProductPricing(product.remoteId))
                }
            )
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        val inventoryGroup = when {
            product.manageStock -> mapOf(
                Pair(resources.getString(R.string.product_backorders),
                    ProductBackorderStatus.backordersToDisplayString(resources, product.backorderStatus)),
                Pair(resources.getString(R.string.product_stock_quantity),
                    StringUtils.formatCount(product.stockQuantity)),
                Pair(resources.getString(R.string.product_sku), product.sku)
            )
            product.sku.isNotEmpty() -> mapOf(
                Pair(resources.getString(R.string.product_sku), product.sku),
                Pair(resources.getString(R.string.product_stock_status),
                    ProductStockStatus.stockStatusToDisplayString(resources, product.stockStatus))
            )
            else -> mapOf(Pair("", resources.getString(R.string.product_inventory_empty)))
        }

        items.addPropertyIfNotEmpty(
            PropertyGroup(
                R.string.product_inventory,
                inventoryGroup,
                R.drawable.ic_gridicons_list_checkmark,
                product.manageStock || product.sku.isNotEmpty()
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductInventory(product.remoteId),
                    Stat.PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
                )
            }
        )

        if (!product.isVirtual) {
            val weightWithUnits = product.getWeightWithUnits(parameters.weightUnit)
            val sizeWithUnits = product.getSizeWithUnits(parameters.dimensionUnit)
            val hasShippingInfo = weightWithUnits.isNotEmpty() ||
                sizeWithUnits.isNotEmpty() ||
                product.shippingClass.isNotEmpty()
            val shippingGroup = if (hasShippingInfo) {
                mapOf(
                    Pair(resources.getString(R.string.product_weight), weightWithUnits),
                    Pair(resources.getString(R.string.product_dimensions), sizeWithUnits),
                    Pair(
                        resources.getString(R.string.product_shipping_class),
                        viewModel.getShippingClassByRemoteShippingClassId(product.shippingClassId)
                    )
                )
            } else mapOf(Pair("", resources.getString(R.string.product_shipping_empty)))

            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_shipping,
                    shippingGroup,
                    R.drawable.ic_gridicons_shipping,
                    hasShippingInfo
                ) {
                    viewModel.onEditProductCardClicked(
                        ViewProductShipping(product.remoteId),
                        Stat.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
                    )
                }
            )
        }

        if (FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()) {
            val shortDescription = if (product.shortDescription.isEmpty()) {
                resources.getString(R.string.product_short_description_empty)
            } else {
                product.shortDescription
            }

            items.addPropertyIfNotEmpty(
                ComplexProperty(
                    R.string.product_short_description,
                    shortDescription,
                    R.drawable.ic_gridicons_align_left
                ) {
                    viewModel.onEditProductCardClicked(
                        ViewProductShortDescriptionEditor(
                            product.shortDescription,
                            resources.getString(R.string.product_short_description)
                        ),
                        Stat.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
                    )
                }
            )
        }

        return ProductPropertyCard(type = SECONDARY, properties = items)
    }
    /**
     * Existing product detail card UI which that will be replaced by the new design once
     * Product Release 1 changes are completed.
     */
    private fun getPricingAndInventoryCard(product: Product): ProductPropertyCard {
        val items = mutableListOf<ProductProperty>()

        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.regularPrice != null || product.salePrice != null

        val title: String
        if (hasPricingInfo) {
            title = resources.getString(R.string.product_pricing_and_inventory)
            // when there's a sale price show price & sales price as a group, otherwise show price separately
            if (product.isOnSale) {
                val group = mapOf(
                    resources.getString(R.string.product_regular_price)
                        to formatCurrency(product.regularPrice, parameters.currencyCode),
                    resources.getString(R.string.product_sale_price)
                        to formatCurrency(product.salePrice, parameters.currencyCode)
                )
                items.addPropertyIfNotEmpty(PropertyGroup(R.string.product_price, group))
            } else {
                items.addPropertyIfNotEmpty(
                    ComplexProperty(
                        R.string.product_price,
                        formatCurrency(product.regularPrice, parameters.currencyCode)
                    )
                )
            }
        } else {
            title = resources.getString(R.string.product_inventory)
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        if (product.manageStock) {
            val group = mapOf(
                Pair(resources.getString(R.string.product_stock_status),
                    ProductStockStatus.stockStatusToDisplayString(resources, product.stockStatus)
                ),
                Pair(resources.getString(R.string.product_backorders),
                    ProductBackorderStatus.backordersToDisplayString(resources, product.backorderStatus)
                ),
                Pair(resources.getString(R.string.product_stock_quantity),
                    StringUtils.formatCount(product.stockQuantity)
                ),
                Pair(resources.getString(R.string.product_sku), product.sku)
            )
            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_inventory,
                    group
                )
            )
        } else {
            items.addPropertyIfNotEmpty(
                ComplexProperty(
                    R.string.product_sku,
                    product.sku
                )
            )
        }

        return ProductPropertyCard(PRICING, title, items)
    }

    private fun getPurchaseDetailsCard(product: Product): ProductPropertyCard {
        val items = mutableListOf<ProductProperty>()

        // shipping group is part of the secondary card if edit product is enabled
        if (!isAddEditProductRelease1Enabled(product.type)) {
            val shippingGroup = mapOf(
                Pair(resources.getString(R.string.product_weight), product.getWeightWithUnits(parameters.weightUnit)),
                Pair(resources.getString(R.string.product_size), product.getSizeWithUnits(parameters.dimensionUnit)),
                Pair(resources.getString(R.string.product_shipping_class), product.shippingClass)
            )
            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_shipping,
                    shippingGroup
                )
            )
        }

        if (product.isDownloadable) {
            val limit = if (product.downloadLimit > 0) String.format(
                resources.getString(R.string.product_download_limit_count),
                product.downloadLimit
            ) else ""
            val expiry = if (product.downloadExpiry > 0) String.format(
                resources.getString(R.string.product_download_expiry_days),
                product.downloadExpiry
            ) else ""

            val downloadGroup = mapOf(
                Pair(resources.getString(R.string.product_downloadable_files), product.fileCount.toString()),
                Pair(resources.getString(R.string.product_download_limit), limit),
                Pair(resources.getString(R.string.product_download_expiry), expiry)
            )
            items.addPropertyIfNotEmpty(
                PropertyGroup(
                    R.string.product_downloads,
                    downloadGroup
                )
            )
        }

        // if add/edit products is enabled, purchase note appears in product settings
        if (product.purchaseNote.isNotBlank() && !isAddEditProductRelease1Enabled(product.type)) {
            items.add(
                ReadMore(
                    R.string.product_purchase_note,
                    product.purchaseNote
                )
            )
        }

        return ProductPropertyCard(PURCHASE_DETAILS, resources.getString(R.string.product_purchase_details), items)
    }
}
