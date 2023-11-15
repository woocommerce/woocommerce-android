package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

object PriceUtils {
    /**
     *  Returns price & sales price as a group if we have pricing info,
     *  otherwise provide option to add pricing info for the product
     */
    fun getPriceGroup(
        parameters: SiteParameters,
        resources: ResourceProvider,
        currencyFormatter: CurrencyFormatter,
        pricingData: PricingData
    ): Map<String, String> = with(pricingData) {
        if (!regularPrice.isSet()) {
            return mapOf("" to resources.getString(R.string.product_price_empty))
        }

        val pricingGroup = mutableMapOf<String, String>()
        // regular product price
        val priceFormatted = formatCurrency(
            regularPrice,
            parameters.currencyCode,
            currencyFormatter
        )
        if (isSubscription && subscriptionInterval != null && subscriptionPeriod != null) {
            pricingGroup[resources.getString(R.string.product_regular_price)] = resources.getString(
                R.string.product_subscription_description,
                priceFormatted,
                subscriptionInterval.toString(),
                subscriptionPeriod.getPeriodString(resources, subscriptionInterval)
            )
        } else {
            pricingGroup[resources.getString(R.string.product_regular_price)] = priceFormatted
        }

        if (isSubscription && subscriptionSignUpFee.isSet() && subscriptionSignUpFee.isNotEqualTo(BigDecimal.ZERO)) {
            // display subscription sign up fee if it's set
            pricingGroup[resources.getString(R.string.subscription_sign_up_fee)] = formatCurrency(
                subscriptionSignUpFee,
                parameters.currencyCode,
                currencyFormatter
            )
        }

        // display product sale price if it's on sale
        if (salePrice.isSet()) {
            pricingGroup[resources.getString(R.string.product_sale_price)] = formatCurrency(
                salePrice,
                parameters.currencyCode,
                currencyFormatter
            )
        }

        // display product sale dates
        if (isSaleScheduled != null) {
            val saleDates = getProductSaleDates(
                dateOnSaleFrom = saleStartDate,
                dateOnSaleTo = saleEndDate,
                resources = resources
            )
            saleDates?.let {
                pricingGroup[resources.getString(R.string.product_sale_dates)] = it
            }
        }
        return pricingGroup
    }

    fun formatCurrency(amount: BigDecimal?, currencyCode: String?, currencyFormatter: CurrencyFormatter): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    private fun getProductSaleDates(dateOnSaleFrom: Date?, dateOnSaleTo: Date?, resources: ResourceProvider): String? {
        return when {
            // both dates are set
            (dateOnSaleFrom != null && dateOnSaleTo != null) -> {
                val formattedFromDate = if (DateTimeUtils.isSameYear(dateOnSaleFrom, dateOnSaleTo)) {
                    dateOnSaleFrom.formatToMMMdd()
                } else {
                    dateOnSaleFrom.formatToMMMddYYYY()
                }
                resources.getString(
                    R.string.product_sale_date_from_to,
                    formattedFromDate,
                    dateOnSaleTo.formatToMMMddYYYY()
                )
            }
            // only start date is set
            dateOnSaleFrom != null -> {
                resources.getString(R.string.product_sale_date_from, dateOnSaleFrom.formatToMMMddYYYY())
            }
            // only end date is set
            dateOnSaleTo != null -> {
                resources.getString(R.string.product_sale_date_to, dateOnSaleTo.formatToMMMddYYYY())
            }

            else -> null
        }
    }
}
