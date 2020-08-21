package com.woocommerce.android.ui.products.variations

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_INVENTORY_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_SHIPPING_SETTINGS_TAPPED
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductInventoryViewModel.InventoryData
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductShippingViewModel.ShippingData
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.Switch
import com.woocommerce.android.ui.products.models.ProductProperty.Warning
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewDescriptionEditor
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewInventory
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewPricing
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewShipping
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag.PRODUCT_RELEASE_M3
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.FormatUtils

class VariationDetailCardBuilder(
    private val viewModel: VariationDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters
) {
    private lateinit var originalSku: String

    fun buildPropertyCards(variation: ProductVariation, originalSku: String): List<ProductPropertyCard> {
        this.originalSku = originalSku

        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(variation))

        return cards
    }

    private fun getPrimaryCard(variation: ProductVariation): ProductPropertyCard {
        return ProductPropertyCard(
            type = PRIMARY,
            properties = listOf(
                variation.description(),
                variation.price(),
                variation.warning(),
                variation.visibility(),
                variation.inventory(),
                variation.shipping()
            ).filterNotEmpty()
        )
    }

    private fun ProductVariation.description(): ProductProperty {
        val variationDescription = this.description
        val description = if (variationDescription.isEmpty()) {
            resources.getString(string.product_description_empty)
        } else {
            variationDescription
        }

        val onClick = {
            viewModel.onEditVariationCardClicked(
                ViewDescriptionEditor(
                    variationDescription, resources.getString(string.product_description)
                ),
                Stat.PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED
            )
        }

        return ComplexProperty(
            string.product_description,
            description,
            drawable.ic_gridicons_align_left,
            variationDescription.isNotEmpty(),
            onClick = if (PRODUCT_RELEASE_M3.isEnabled()) onClick else null
        )
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

        return if (PRODUCT_RELEASE_M3.isEnabled()) {
            Switch(visibility, isVisible, visibilityIcon) {
                viewModel.onVariationVisibilitySwitchChanged(it)
            }
        } else {
            Switch(visibility, isVisible, visibilityIcon)
        }
    }

    private fun ProductVariation.warning(): ProductProperty? {
        return if (regularPrice.isNotSet() && this.isVisible) {
            Warning(resources.getString(string.variation_detail_price_warning))
        } else {
            null
        }
    }

    // If we have pricing info, show price & sales price as a group,
    // otherwise provide option to add pricing info for the variation
    private fun ProductVariation.price(): ProductProperty? {
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

        return if (regularPrice.isSet() || PRODUCT_RELEASE_M3.isEnabled()) {
            val onClick = {
                viewModel.onEditVariationCardClicked(
                    ViewPricing(
                        PricingData(
                            isSaleScheduled = isSaleScheduled,
                            saleStartDate = saleStartDateGmt,
                            saleEndDate = saleEndDateGmt,
                            regularPrice = regularPrice,
                            salePrice = salePrice
                        )
                    ),
                    PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
                )
            }

            val isWarningVisible = regularPrice.isNotSet() && this.isVisible
            PropertyGroup(
                string.product_price,
                pricingGroup,
                drawable.ic_gridicons_money,
                showTitle = regularPrice.isSet(),
                isHighlighted = isWarningVisible,
                isDividerVisible = !isWarningVisible,
                onClick = if (PRODUCT_RELEASE_M3.isEnabled()) onClick else null
            )
        } else {
            null
        }
    }

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
                    Pair(resources.getString(string.product_dimensions), sizeWithUnits),
                    Pair(
                        resources.getString(string.product_shipping_class),
                        viewModel.getShippingClassByRemoteShippingClassId(this.shippingClassId)
                    )
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
                    FormatUtils.formatInt(this.stockQuantity)
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

        val onClick = {
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

        return PropertyGroup(
            R.string.product_inventory,
            inventoryGroup,
            R.drawable.ic_gridicons_list_checkmark,
            true,
            onClick = if (PRODUCT_RELEASE_M3.isEnabled()) onClick else null
        )
    }
}
