package com.woocommerce.android.ui.products

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.filterNotEmpty
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductVariantViewModel.Parameters
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

class ProductVariantCardBuilder(
    private val viewModel: ProductVariantViewModel,
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val parameters: Parameters
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
                variation.price(),
                variation.type()
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
        val visibility: String
        @DrawableRes val visibilityIcon: Int
        if (this.status == PUBLISH) {
            visibility = resources.getString(R.string.product_variation_visible)
            visibilityIcon = R.drawable.ic_gridicons_visible
        } else {
            visibility = resources.getString(R.string.product_variant_hidden)
            visibilityIcon = R.drawable.ic_gridicons_not_visible
        }

        return ComplexProperty(value = visibility, icon = visibilityIcon, showTitle = false)
    }

    // If we have pricing info, show price & sales price as a group,
    // otherwise provide option to add pricing info for the variation
    private fun ProductVariant.price(): ProductProperty {
        val hasPricingInfo = this.regularPrice != null || this.salePrice != null
        val pricingGroup = mutableMapOf<String, String>()
        if (hasPricingInfo) {
            // regular variation price
            pricingGroup[resources.getString(R.string.product_regular_price)] = formatCurrency(
                this.regularPrice,
                parameters.currencyCode
            )
            // display variation sale price if it's on sale
            if (this.isOnSale) {
                pricingGroup[resources.getString(R.string.product_sale_price)] = formatCurrency(
                    this.salePrice,
                    parameters.currencyCode
                )
            }

            // display variation sale dates using the site's timezone, if available
            if (this.isSaleScheduled) {
                var dateOnSaleFrom = this.saleStartDateGmt?.let {
                    DateUtils.offsetGmtDate(it, parameters.gmtOffset)
                }
                val dateOnSaleTo = this.saleEndDateGmt?.let {
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

    private fun ProductVariant.type(): ProductProperty {
        var type = if (this.isVirtual)
            resources.getString(R.string.product_type_virtual)
        else
            resources.getString(R.string.product_type_physical)

        if (this.isDownloadable)
            type += ", ${resources.getString(R.string.product_type_downloadable)}"

        return ComplexProperty(
            R.string.product_type,
            resources.getString(R.string.product_type_description, type).capitalize(),
            R.drawable.ic_product
        )
        // TODO: This will be used once the variants are editable
//            {
//                viewModel.onEditVariationCardClicked(
//                    ViewProductPricing(variation.remoteVariationId),
//                    PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
//                )
//            }
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
}
