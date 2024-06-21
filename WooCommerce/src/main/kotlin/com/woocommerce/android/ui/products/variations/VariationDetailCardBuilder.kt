package com.woocommerce.android.ui.products.variations

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_VIEW_INVENTORY_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_VIEW_SHIPPING_SETTINGS_TAPPED
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.Switch
import com.woocommerce.android.ui.products.models.ProductProperty.Warning
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.subscriptions.expirationDisplayValue
import com.woocommerce.android.ui.products.subscriptions.trialDisplayValue
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewAttributes
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewDescriptionEditor
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewInventory
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewPricing
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewProductQuantityRules
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewProductSubscriptionExpiration
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewShipping
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewVariationSubscriptionTrial
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.utils.putIfNotNull

class VariationDetailCardBuilder(
    private val viewModel: VariationDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters,
) {
    private lateinit var originalSku: String
    private var parentProduct: Product? = null

    suspend fun buildPropertyCards(
        variation: ProductVariation,
        originalSku: String,
        parentProduct: Product?
    ): List<ProductPropertyCard> {
        this.originalSku = originalSku
        this.parentProduct = parentProduct

        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(variation))
        cards.addIfNotEmpty(getSecondaryCard(variation))

        return cards
    }

    private suspend fun getSecondaryCard(variation: ProductVariation): ProductPropertyCard {
        return when (variation) {
            is SubscriptionProductVariation -> getVariableSubscriptionSecondaryCards(variation)
            else -> getDefaultSecondaryCards(variation)
        }
    }

    private suspend fun getDefaultSecondaryCards(variation: ProductVariation): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                variation.price(),
                variation.warning(),
                variation.attributes(),
                variation.quantityRules(),
                variation.visibility(),
                variation.inventory(),
                variation.shipping()
            ).filterNotEmpty()
        )
    }

    private suspend fun getVariableSubscriptionSecondaryCards(
        variation: SubscriptionProductVariation
    ): ProductPropertyCard {
        return ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                variation.warning(),
                variation.price(),
                variation.subscriptionExpirationDate(),
                variation.subscriptionTrial(),
                variation.attributes(),
                variation.quantityRules(),
                variation.visibility(),
                variation.inventory(),
                variation.shipping()
            ).filterNotEmpty()
        )
    }

    private fun getPrimaryCard(variation: ProductVariation): ProductPropertyCard {
        return ProductPropertyCard(
            type = PRIMARY,
            properties = listOf(
                variation.title(),
                variation.description()
            ).filterNotEmpty()
        )
    }

    private fun ProductVariation.title(): ProductProperty {
        return Editable(
            string.product_detail_title_hint,
            parentProduct?.name ?: getName(parentProduct),
            isReadOnly = true
        )
    }

    private fun ProductVariation.description(): ProductProperty {
        val variationDescription = this.description
        val description = if (variationDescription.isEmpty()) {
            resources.getString(string.product_description_empty)
        } else {
            variationDescription
        }

        return ComplexProperty(
            string.product_description,
            description,
            showTitle = variationDescription.isNotEmpty()
        ) {
            viewModel.onEditVariationCardClicked(
                ViewDescriptionEditor(
                    variationDescription,
                    resources.getString(string.product_description)
                ),
                AnalyticsEvent.PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED
            )
        }
    }

    private fun ProductVariation.visibility(): ProductProperty {
        @StringRes val visibility: Int

        @DrawableRes val visibilityIcon: Int
        if (this.isVisible) {
            visibility = string.product_variation_enabled
            visibilityIcon = drawable.ic_gridicons_visible
        } else {
            visibility = string.product_variation_disabled
            visibilityIcon = drawable.ic_gridicons_not_visible
        }

        return Switch(visibility, isVisible, visibilityIcon) {
            viewModel.onVariationVisibilitySwitchChanged(it)
        }
    }

    private fun ProductVariation.warning(): ProductProperty? {
        return if (regularPrice.isNotSet() && this.isVisible) {
            Warning(resources.getString(string.variation_detail_price_warning))
        } else {
            null
        }
    }

    private fun ProductVariation.price(): ProductProperty {
        val subscriptionDetails = (this as? SubscriptionProductVariation)?.subscriptionDetails
        val pricingData = PricingData(
            isSaleScheduled = isSaleScheduled,
            saleStartDate = saleStartDateGmt,
            saleEndDate = saleEndDateGmt,
            regularPrice = regularPrice,
            salePrice = salePrice,
            isSubscription = this is SubscriptionProductVariation,
            subscriptionPeriod = subscriptionDetails?.period,
            subscriptionInterval = subscriptionDetails?.periodInterval,
            subscriptionSignUpFee = subscriptionDetails?.signUpFee
        )
        val pricingGroup = PriceUtils.getPriceGroup(
            parameters,
            resources,
            currencyFormatter,
            pricingData
        )

        val isWarningVisible = regularPrice.isNotSet() && this.isVisible

        return PropertyGroup(
            string.product_price,
            pricingGroup,
            drawable.ic_gridicons_money,
            showTitle = regularPrice.isSet(),
            isHighlighted = isWarningVisible,
            isDividerVisible = !isWarningVisible
        ) {
            viewModel.onEditVariationCardClicked(
                ViewPricing(pricingData),
                PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
            )
        }
    }

    private fun ProductVariation.attributes() =
        PropertyGroup(
            title = string.product_attributes,
            properties = mutableMapOf<String, String>()
                .let { map ->
                    attributes
                        .filter { it.name != null && it.option != null }
                        .map { Pair(it.name!!, it.option!!) }
                        .let { map.apply { putAll(it) } }
                }.also { map ->
                    parentProduct?.variationEnabledAttributes
                        ?.filter { map.containsKey(it.name).not() }
                        ?.map { Pair(it.name, resources.getString(string.product_any_attribute_hint)) }
                        ?.let { map.apply { putAll(it) } }
                },
            icon = drawable.ic_gridicons_customize,
            onClick = {
                viewModel.onEditVariationCardClicked(
                    ViewAttributes(
                        remoteProductId,
                        remoteVariationId
                    ),
                    AnalyticsEvent.PRODUCT_VARIATION_DETAILS_ATTRIBUTES_TAPPED
                )
            }
        )

    private fun ProductVariation.shipping(): ProductProperty? {
        return if (!this.isVirtual) {
            val weightWithUnits = this.getWeightWithUnits(parameters.weightUnit)
            val sizeWithUnits = this.getSizeWithUnits(parameters.dimensionUnit)
            val hasShippingInfo = weightWithUnits.isNotEmpty() ||
                sizeWithUnits.isNotEmpty() ||
                this.shippingClass.isNotEmpty()
            val shippingGroup = if (hasShippingInfo) {
                mapOf(
                    Pair(resources.getString(string.product_weight), weightWithUnits),
                    Pair(resources.getString(string.product_dimensions), sizeWithUnits)
                )
            } else {
                mapOf(Pair("", resources.getString(string.product_shipping_empty)))
            }

            PropertyGroup(
                string.product_shipping,
                shippingGroup,
                drawable.ic_gridicons_shipping,
                hasShippingInfo
            ) {
                viewModel.onEditVariationCardClicked(
                    ViewShipping(
                        ShippingData(
                            weight,
                            length,
                            width,
                            height,
                            shippingClass,
                            shippingClassId
                        )
                    ),
                    PRODUCT_VARIATION_VIEW_SHIPPING_SETTINGS_TAPPED
                )
            }
        } else {
            null
        }
    }

    private fun ProductVariation.inventory(): ProductProperty {
        val inventoryGroup = when {
            this.isStockManaged -> mapOf(
                Pair(
                    resources.getString(R.string.product_backorders),
                    ProductBackorderStatus.backordersToDisplayString(resources, this.backorderStatus)
                ),
                Pair(
                    resources.getString(R.string.product_stock_quantity),
                    StringUtils.formatCountDecimal(this.stockQuantity)

                ),
                Pair(resources.getString(R.string.product_sku), this.sku)
            )

            this.sku.isNotEmpty() -> mapOf(
                Pair(resources.getString(R.string.product_sku), this.sku),
                Pair(
                    resources.getString(R.string.product_stock_status),
                    ProductStockStatus.stockStatusToDisplayString(resources, this.stockStatus)
                )
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
            viewModel.onEditVariationCardClicked(
                ViewInventory(
                    InventoryData(
                        sku = this.sku,
                        isStockManaged = this.isStockManaged,
                        stockStatus = this.stockStatus,
                        stockQuantity = this.stockQuantity,
                        backorderStatus = this.backorderStatus
                    ),
                    originalSku
                ),
                PRODUCT_VARIATION_VIEW_INVENTORY_SETTINGS_TAPPED
            )
        }
    }

    private suspend fun ProductVariation.quantityRules(): ProductProperty? {
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
                viewModel.onEditVariationCardClicked(
                    ViewProductQuantityRules(rules, AnalyticsEvent.PRODUCT_VARIATION_QUANTITY_RULES_DONE_BUTTON_TAPPED),
                    AnalyticsEvent.PRODUCT_VARIATION_VIEW_QUANTITY_RULES_TAPPED
                )
            }
        )
    }

    private fun SubscriptionProductVariation.subscriptionExpirationDate(): ProductProperty? =
        this.subscriptionDetails?.let { subscription ->
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
                    viewModel.onEditVariationCardClicked(
                        ViewProductSubscriptionExpiration(subscription),
                        AnalyticsEvent.PRODUCT_VARIATION_VIEW_SUBSCRIPTION_EXPIRATION_TAPPED
                    )
                }
            )
        }

    private fun SubscriptionProductVariation.subscriptionTrial(): ProductProperty? =
        this.subscriptionDetails?.let { subscription ->
            PropertyGroup(
                title = string.product_subscription_free_trial_title,
                icon = drawable.ic_hourglass_empty,
                properties = mapOf(
                    resources.getString(string.subscription_free_trial) to subscription.trialDisplayValue(resources)
                ),
                showTitle = true,
                onClick = {
                    viewModel.onEditVariationCardClicked(
                        ViewVariationSubscriptionTrial(subscription),
                        AnalyticsEvent.PRODUCT_VARIATION_VIEW_SUBSCRIPTION_FREE_TRIAL_TAPPED
                    )
                }
            )
        }
}
