package com.woocommerce.android.ui.products.variations

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
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
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewPricing
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal

class VariationDetailCardBuilder(
    private val viewModel: VariationDetailViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters
) {
    fun buildPropertyCards(variation: ProductVariation): List<ProductPropertyCard> {
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
            resources.getString(string.product_description)
        } else {
            variationDescription
        }

        return ComplexProperty(
            string.product_description,
            description,
            drawable.ic_gridicons_align_left,
            variationDescription.isNotEmpty()
        ) {
            viewModel.onEditVariationCardClicked(
                ViewDescriptionEditor(
                    variationDescription, resources.getString(string.product_description)
                ),
                Stat.PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED
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

        return Switch(visibility, this.isVisible, visibilityIcon) {
            viewModel.onVariationChanged(isVisible = it)
        }
    }

    private fun ProductVariation.warning(): ProductProperty? {
        val hasPricingInfo = !(this.regularPrice isEquivalentTo BigDecimal.ZERO) ||
            !(this.regularPrice isEquivalentTo BigDecimal.ZERO)

        return if (!hasPricingInfo && this.isVisible) {
            Warning(resources.getString(string.variation_detail_price_warning))
        } else {
            null
        }
    }

    // If we have pricing info, show price & sales price as a group,
    // otherwise provide option to add pricing info for the variation
    private fun ProductVariation.price(): ProductProperty {
        val hasPricingInfo = !(this.regularPrice isEquivalentTo BigDecimal.ZERO) ||
            !(this.regularPrice isEquivalentTo BigDecimal.ZERO)
        val pricingGroup = PriceUtils.getPriceGroup(
            hasPricingInfo,
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
            string.product_price,
            pricingGroup,
            drawable.ic_gridicons_money,
            showTitle = hasPricingInfo,
            isDividerVisible = hasPricingInfo || !this.isVisible,
            isHighlighted = !hasPricingInfo && this.isVisible
        ) {
                viewModel.onEditVariationCardClicked(
                    ViewPricing(PricingData(
                        isSaleScheduled = isSaleScheduled,
                        saleStartDate = saleStartDateGmt,
                        saleEndDate = saleEndDateGmt,
                        regularPrice = regularPrice,
                        salePrice = salePrice
                    )),
                    PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED
                )
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
            } else mapOf(Pair("", resources.getString(string.product_shipping_empty)))

            PropertyGroup(
                string.product_shipping,
                shippingGroup,
                drawable.ic_gridicons_shipping,
                hasShippingInfo
            )
            // TODO: This will be used once the variants are editable
//            {
//                viewModel.onEditProductCardClicked(
//                    ViewProductShipping(this.remoteId),
//                    PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
//                )
//            }
        } else {
            null
        }
    }

    private fun ProductVariation.inventory(): ProductProperty {
        return ComplexProperty(
            string.product_inventory,
            ProductStockStatus.stockStatusToDisplayString(
                resources,
                this.stockStatus
            ),
            drawable.ic_gridicons_list_checkmark,
            true
        )
        // TODO: This will be used once the variants are editable
//        {
//            viewModel.onEditProductCardClicked(
//                ViewProductInventory(this.remoteId),
//                PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED
//            )
//        }
    }
}
