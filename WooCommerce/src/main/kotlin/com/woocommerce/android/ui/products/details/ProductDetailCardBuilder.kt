package com.woocommerce.android.ui.products.details

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_ATTRIBUTE_EDIT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.blaze.IsProductCurrentlyPromoted
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductNavigationTarget
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
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductQuantityRules
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductReviews
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSubscriptionExpiration
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSubscriptionFreeTrial
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTags
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductTypes
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVariations
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ProductType.BUNDLE
import com.woocommerce.android.ui.products.ProductType.COMPOSITE
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.OTHER
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.SUBSCRIPTION
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE_SUBSCRIPTION
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Button
import com.woocommerce.android.ui.products.models.ProductProperty.Button.Link
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.subscriptions.expirationDisplayValue
import com.woocommerce.android.ui.products.subscriptions.trialDisplayValue
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.utils.putIfNotNull
import java.math.BigDecimal

@Suppress("LargeClass", "LongParameterList")
class ProductDetailCardBuilder(
    private val viewModel: ProductDetailViewModel,
    private val selectedSite: SelectedSite,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters,
    private val addonRepository: AddonRepository,
    private val variationRepository: VariationRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val isProductCurrentlyPromoted: IsProductCurrentlyPromoted,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private var blazeCtaShownTracked = false
    private lateinit var originalSku: String

    companion object {
        const val MAXIMUM_TIMES_TO_SHOW_TOOLTIP = 3
    }

    private val onTooltipDismiss = { appPrefsWrapper.isAIProductDescriptionTooltipDismissed = true }

    suspend fun buildPropertyCards(product: Product, originalSku: String): List<ProductPropertyCard> {
        this.originalSku = originalSku

        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(product))

        cards.addIfNotEmpty(getBlazeCard(product))

        when (product.productType) {
            SIMPLE -> cards.addIfNotEmpty(getSimpleProductCard(product))
            VARIABLE -> cards.addIfNotEmpty(getVariableProductCard(product))
            GROUPED -> cards.addIfNotEmpty(getGroupedProductCard(product))
            EXTERNAL -> cards.addIfNotEmpty(getExternalProductCard(product))
            SUBSCRIPTION -> cards.addIfNotEmpty(getSubscriptionProductCard(product))
            VARIABLE_SUBSCRIPTION -> cards.addIfNotEmpty(getVariableSubscriptionProductCard(product))
            BUNDLE -> cards.addIfNotEmpty(getBundleProductsCard(product))
            COMPOSITE -> cards.addIfNotEmpty(getCompositeProductsCard(product))
            else -> cards.addIfNotEmpty(getOtherProductCard(product))
        }

        return cards
    }

    private fun getPrimaryCard(product: Product): ProductPropertyCard {
        val showTooltip = product.description.isEmpty() &&
            !appPrefsWrapper.isAIProductDescriptionTooltipDismissed &&
            appPrefsWrapper.getAIDescriptionTooltipShownNumber() <= MAXIMUM_TIMES_TO_SHOW_TOOLTIP
        return ProductPropertyCard(
            type = PRIMARY,
            properties = (
                listOf(product.title()) +
                    product.description(
                        showAIButton = selectedSite.get().isEligibleForAI,
                        showTooltip = showTooltip,
                        onWriteWithAIClicked = viewModel::onWriteWithAIClicked,
                        onLearnMoreClicked = viewModel::onLearnMoreClicked
                    )
                ).filterNotEmpty()
        )
    }

    private suspend fun getBlazeCard(product: Product): ProductPropertyCard? {
        val isProductPublic = product.status == ProductStatus.PUBLISH &&
            viewModel.getProductVisibility() == ProductVisibility.PUBLIC

        @Suppress("ComplexCondition")
        if (!isBlazeEnabled() ||
            !isProductPublic ||
            viewModel.isProductUnderCreation ||
            isProductCurrentlyPromoted(product.remoteId.toString())
        ) {
            return null
        }

        if (!blazeCtaShownTracked) {
            analyticsTrackerWrapper.track(
                stat = BLAZE_ENTRY_POINT_DISPLAYED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.PRODUCT_DETAIL_PROMOTE_BUTTON.trackingName
                )
            )
            blazeCtaShownTracked = true
        }
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                ProductProperty.Link(
                    title = R.string.product_details_blaze_card,
                    icon = R.drawable.ic_blaze,
                    isDividerVisible = false,
                    onClick = viewModel::onBlazeClicked
                )
            )
        )
    }

    private suspend fun getSimpleProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(SIMPLE),
                product.addons(),
                product.quantityRules(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.downloads(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getGroupedProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.groupedProducts(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(GROUPED),
                product.addons(),
                product.quantityRules(),
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
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.externalLink(),
                product.inventory(EXTERNAL),
                product.addons(),
                product.quantityRules(),
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
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(VARIABLE),
                product.addons(),
                product.quantityRules(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getSubscriptionProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.price(),
                product.subscriptionExpirationDate(),
                product.subscriptionTrial(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(SIMPLE),
                product.addons(),
                product.quantityRules(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.downloads(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getVariableSubscriptionProductCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.warning(),
                product.variations(),
                product.variationAttributes(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(VARIABLE),
                product.addons(),
                product.quantityRules(),
                product.shipping(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getBundleProductsCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.bundleProducts(),
                product.price(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(SIMPLE),
                product.addons(),
                product.quantityRules(),
                product.categories(),
                product.tags(),
                product.shortDescription(),
                product.linkedProducts(),
                product.productType()
            ).filterNotEmpty()
        )
    }

    private suspend fun getCompositeProductsCard(product: Product): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                product.componentProducts(),
                product.price(),
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.inventory(SIMPLE),
                product.addons(),
                product.quantityRules(),
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
                if (viewModel.isProductUnderCreation) null else product.productReviews(),
                product.addons(),
                product.quantityRules(),
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
                ),
                Pair(
                    resources.getString(string.subscription_one_time_shipping),
                    if (subscription?.oneTimeShipping == true) {
                        resources.getString(string.subscription_one_time_shipping_enabled)
                    } else {
                        ""
                    }
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
                            weight = weight,
                            length = length,
                            width = width,
                            height = height,
                            shippingClassSlug = shippingClass,
                            shippingClassId = shippingClassId,
                            subscriptionShippingData = if (productType == SUBSCRIPTION ||
                                this.productType == VARIABLE_SUBSCRIPTION
                            ) {
                                ShippingData.SubscriptionShippingData(
                                    oneTimeShipping = subscription?.oneTimeShipping ?: false,
                                    canEnableOneTimeShipping = if (productType == SUBSCRIPTION) {
                                        subscription?.supportsOneTimeShipping ?: false
                                    } else {
                                        // For variable subscription products, we need to check against the variations
                                        variationRepository.getProductVariationList(remoteId).all {
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
        val pricingData = PricingData(
            taxClass = taxClass,
            taxStatus = taxStatus,
            isSaleScheduled = isSaleScheduled,
            saleStartDate = saleStartDateGmt,
            saleEndDate = saleEndDateGmt,
            regularPrice = regularPrice,
            salePrice = salePrice,
            isSubscription = this.productType == SUBSCRIPTION,
            subscriptionPeriod = subscription?.period,
            subscriptionInterval = subscription?.periodInterval,
            subscriptionSignUpFee = subscription?.signUpFee,
        )

        val pricingGroup = PriceUtils.getPriceGroup(
            parameters,
            resources,
            currencyFormatter,
            pricingData
        )

        return PropertyGroup(
            string.product_price,
            pricingGroup,
            drawable.ic_gridicons_money,
            showTitle = this.regularPrice.isSet()
        ) {
            viewModel.onEditProductCardClicked(
                ViewProductPricing(pricingData),
                AnalyticsEvent.PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
            )
        }
    }

    @Suppress("DEPRECATION")
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
            SUBSCRIPTION -> resources.getString(string.product_type_subscription)
            else -> this.type.capitalize() // show the actual product type string for unsupported products
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

    private fun Product.description(
        showAIButton: Boolean,
        showTooltip: Boolean,
        onWriteWithAIClicked: () -> Unit,
        onLearnMoreClicked: () -> Unit
    ): List<ProductProperty> {
        val productDescription = this.description
        val productTitle = this.name
        val showTitle = productDescription.isNotEmpty()
        val description = productDescription.ifEmpty {
            resources.getString(string.product_description_empty)
        }

        val properties = mutableListOf<ProductProperty>()
        properties.add(
            ComplexProperty(
                string.product_description,
                description,
                showTitle = showTitle,
                isDividerVisible = !showAIButton
            ) {
                viewModel.onEditProductCardClicked(
                    ViewProductDescriptionEditor(
                        productDescription,
                        resources.getString(string.product_description),
                        productTitle
                    ),
                    PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED
                )
            }
        )

        if (showAIButton) {
            val tooltip = if (showTooltip) {
                appPrefsWrapper.recordAIDescriptionTooltipShown()

                Button.Tooltip(
                    title = string.ai_product_description_tooltip_title,
                    text = string.ai_product_description_tooltip_message,
                    dismissButtonText = string.ai_product_description_tooltip_dismiss,
                    onDismiss = onTooltipDismiss
                )
            } else {
                null
            }
            properties.add(
                Button(
                    string.product_sharing_write_with_ai,
                    drawable.ic_ai,
                    onClick = onWriteWithAIClicked,
                    tooltip = tooltip,
                    link = Link(
                        string.ai_product_description_learn_more_link,
                        onLearnMoreClicked
                    )
                )
            )
        }
        return properties
    }

    // show product variations only if product type is variable and if there are variations for the product
    private fun Product.variations(): ProductProperty? {
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
                    ViewProductVariations(remoteId = this.remoteId),
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
                analyticsTrackerWrapper.track(
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

    private fun Product.subscriptionExpirationDate(): ProductProperty? =
        this.subscription?.let { subscription ->
            PropertyGroup(
                title = string.product_subscription_expiration_title,
                icon = drawable.ic_calendar_expiration,
                properties = mapOf(
                    resources.getString(string.subscription_expire) to subscription.expirationDisplayValue(
                        resources
                    )
                ),
                showTitle = true,
                onClick = {
                    viewModel.onEditProductCardClicked(
                        ViewProductSubscriptionExpiration(subscription),
                        AnalyticsEvent.PRODUCT_DETAILS_VIEW_SUBSCRIPTION_EXPIRATION_TAPPED
                    )
                }
            )
        }

    private fun Product.subscriptionTrial(): ProductProperty? =
        this.subscription?.let { subscription ->
            PropertyGroup(
                title = string.product_subscription_free_trial_title,
                icon = drawable.ic_hourglass_empty,
                properties = mapOf(
                    resources.getString(string.subscription_free_trial) to subscription.trialDisplayValue(resources)
                ),
                showTitle = true,
                onClick = {
                    viewModel.onEditProductCardClicked(
                        ViewProductSubscriptionFreeTrial(subscription),
                        AnalyticsEvent.PRODUCT_DETAILS_VIEW_SUBSCRIPTION_FREE_TRIAL_TAPPED
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

    private suspend fun Product.quantityRules(): ProductProperty? {
        val rules = QuantityRules(this.minAllowedQuantity, this.maxAllowedQuantity, this.groupOfQuantity)

        val properties = buildMap {
            putIfNotNull(resources.getString(string.min_quantity) to rules.min?.toString())
            putIfNotNull(resources.getString(string.max_quantity) to rules.max?.toString())
            if (size < 2) putIfNotNull(resources.getString(string.group_of) to rules.groupOf?.toString())
        }

        return PropertyGroup(
            title = string.product_quantity_rules_title,
            icon = drawable.ic_gridicons_product,
            properties = properties,
            showTitle = true,
            onClick = {
                viewModel.onEditProductCardClicked(
                    ViewProductQuantityRules(rules, AnalyticsEvent.PRODUCT_DETAIL_QUANTITY_RULES_DONE_BUTTON_TAPPED),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_QUANTITY_RULES_TAPPED
                )
            }
        )
    }

    private suspend fun Product.bundleProducts(): ProductProperty? {
        val bundledProductsSize = viewModel.getBundledProductsSize(this.remoteId)
        return if (bundledProductsSize > 0) {
            val content = StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = bundledProductsSize,
                default = string.product_bundle_multiple_count,
                one = string.product_bundle_single_count
            )

            ComplexProperty(
                string.product_bundle,
                content,
                drawable.ic_widgets
            ) {
                viewModel.onEditProductCardClicked(
                    ProductNavigationTarget.ViewBundleProducts(this.remoteId),
                    AnalyticsEvent.PRODUCT_DETAIL_VIEW_BUNDLED_PRODUCTS_TAPPED
                )
            }
        } else {
            null
        }
    }

    private suspend fun Product.componentProducts(): ProductProperty? {
        val components = viewModel.getComponents(this.remoteId)
        return if (components.isNullOrEmpty()) {
            null
        } else {
            val content = StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = components.size,
                default = string.product_component_multiple_count,
                one = string.product_component_single_count
            )

            ComplexProperty(
                string.product_components,
                content,
                drawable.ic_widgets
            ) {
                viewModel.onEditProductCardClicked(
                    ProductNavigationTarget.ViewProductComponents(components),
                    AnalyticsEvent.PRODUCT_DETAILS_VIEW_COMPONENTS_TAPPED
                )
            }
        }
    }
}

fun ProductStatus?.getBadgeResources(): Pair<Int?, Int?> {
    return if (this == null) {
        Pair(null, null)
    } else {
        when (this) {
            ProductStatus.PUBLISH -> Pair(null, null)
            ProductStatus.PENDING -> Pair(string.product_status_pending, R.color.product_status_badge_pending)
            ProductStatus.PRIVATE -> Pair(string.product_status_privately_published, R.color.product_status_badge_draft)
            else -> Pair(this.stringResource, R.color.product_status_badge_draft)
        }
    }
}
