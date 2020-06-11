package com.woocommerce.android.ui.products

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.Switch
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductVariantCardBuilder(
    private val viewModel: ProductVariantViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: SiteParameters
) {
    fun buildPropertyCards(variation: ProductVariant): List<ProductPropertyCard> {
        val cards = mutableListOf<ProductPropertyCard>()
        cards.addIfNotEmpty(getPrimaryCard(variation))

        return cards
    }

    private fun getPrimaryCard(variation: ProductVariant): ProductPropertyCard {
        return ProductPropertyCard(
            type = PRIMARY,
            properties = listOf(
                variation.visibility(),
                variation.description(),
                variation.price()
            ).filterNotEmpty()
        )
    }

    private fun ProductVariant.description(): ProductProperty? {
        val variationDescription = this.description
        val description = if (variationDescription.isEmpty()) {
            resources.getString(R.string.product_description)
        } else {
            variationDescription
        }

        // TODO: Temporarily hide empty description until it's editable
        return if (variationDescription.isNotEmpty()) {
            ComplexProperty(
                R.string.product_description,
                description,
                R.drawable.ic_gridicons_align_left,
                variationDescription.isNotEmpty()
            )
            // TODO: This will be used once the variants are editable
//            {
//                viewModel.onEditVariationCardClicked(
//                    ViewProductDescriptionEditor(
//                        variationDescription, resources.getString(R.string.product_description)
//                    ),
//                    Stat.PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED
//                )
//            }
        } else {
            null
        }
    }

    private fun ProductVariant.visibility(): ProductProperty {
        @StringRes val visibility: Int
        @DrawableRes val visibilityIcon: Int
        val isOn: Boolean = this.status == PUBLISH
        if (isOn) {
            visibility = R.string.product_variation_visible
            visibilityIcon = R.drawable.ic_gridicons_visible
        } else {
            visibility = R.string.product_variant_hidden
            visibilityIcon = R.drawable.ic_gridicons_not_visible
        }

        return Switch(visibility, isOn, visibilityIcon)
    }

    // If we have pricing info, show price & sales price as a group,
    // otherwise provide option to add pricing info for the variation
    private fun ProductVariant.price(): ProductProperty {
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
        )
        // TODO: This will be used once the variants are editable
//            {
//                viewModel.onEditVariationCardClicked(
//                    ViewProductPricing(this.remoteVariationId),
//                    PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
//                )
//            }
    }
}
