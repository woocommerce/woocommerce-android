package com.woocommerce.android.ui.variations

import androidx.annotation.DrawableRes
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
import com.woocommerce.android.extensions.addIfNotEmpty
import com.woocommerce.android.extensions.addPropertyIfNotEmpty
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDescriptionEditor
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductVariantViewModel
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
        val items = mutableListOf<ProductProperty>()

        val variationDescription = variation.description
        val showTitle = variationDescription.isNotEmpty()
        val description = if (variationDescription.isEmpty()) {
            resources.getString(R.string.product_description)
        } else {
            variationDescription
        }

        val visibility: String
        @DrawableRes val visibilityIcon: Int
        if (variation.status == PUBLISH) {
            visibility = resources.getString(R.string.product_variation_visible)
            visibilityIcon = R.drawable.ic_password_visibility
        } else {
            visibility = resources.getString(R.string.product_variant_hidden)
            visibilityIcon = R.drawable.ic_password_visibility_off
        }
        items.addPropertyIfNotEmpty(
            ComplexProperty(
                value = visibility,
                icon = visibilityIcon,
                showTitle = false
            )
        )

        items.addPropertyIfNotEmpty(
            ComplexProperty(
                R.string.product_description,
                description,
                showTitle = showTitle
            ) {
                viewModel.onEditVariationCardClicked(
                    ViewProductDescriptionEditor(
                        variationDescription, resources.getString(R.string.product_description)
                    ),
                    Stat.PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED
                )
            }
        )

        // If we have pricing info, show price & sales price as a group,
        // otherwise provide option to add pricing info for the variation
        val hasPricingInfo = variation.regularPrice != null || variation.salePrice != null
        val pricingGroup = mutableMapOf<String, String>()
        if (hasPricingInfo) {
            // regular variation price
            pricingGroup[resources.getString(R.string.product_regular_price)] = formatCurrency(
                variation.regularPrice,
                parameters.currencyCode
            )
            // display variation sale price if it's on sale
            if (variation.isOnSale) {
                pricingGroup[resources.getString(R.string.product_sale_price)] = formatCurrency(
                    variation.salePrice,
                    parameters.currencyCode
                )
            }

            // display variation sale dates using the site's timezone, if available
            if (variation.isSaleScheduled) {
                var dateOnSaleFrom = variation.saleStartDateGmt?.let {
                    DateUtils.offsetGmtDate(it, parameters.gmtOffset)
                }
                val dateOnSaleTo = variation.saleEndDateGmt?.let {
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
                string.product_price,
                pricingGroup,
                drawable.ic_gridicons_money,
                hasPricingInfo
            ) {
                viewModel.onEditVariationCardClicked(
                    ViewProductPricing(variation.remoteVariationId),
                    PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED
                )
            }
        )

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
}
