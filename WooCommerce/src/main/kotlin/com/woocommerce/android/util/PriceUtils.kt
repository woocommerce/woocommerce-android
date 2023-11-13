package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
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
        val pricingGroup = mutableMapOf<String, String>()
        if (pricingData.regularPrice.isSet()) {
            // regular product price
            pricingGroup[resources.getString(R.string.product_regular_price)] = formatCurrency(
                regularPrice,
                parameters.currencyCode,
                currencyFormatter
            )
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
                val saleDates = when {
                    // both dates are set
                    (saleStartDate != null && saleEndDate != null) -> {
                        getProductSaleDates(saleStartDate, saleEndDate, resources)
                    }
                    // only start date is set
                    (saleStartDate != null && saleEndDate == null) -> {
                        resources.getString(R.string.product_sale_date_from, saleStartDate.formatToMMMddYYYY())
                    }
                    // only end date is set
                    (saleStartDate == null && saleEndDate != null) -> {
                        resources.getString(R.string.product_sale_date_to, saleEndDate.formatToMMMddYYYY())
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
        return pricingGroup
    }

    fun formatCurrency(amount: BigDecimal?, currencyCode: String?, currencyFormatter: CurrencyFormatter): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    private fun getProductSaleDates(dateOnSaleFrom: Date, dateOnSaleTo: Date, resources: ResourceProvider): String {
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
