package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.extensions.offsetGmtDate
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

object PriceUtils {
    fun getPriceGroup(
        parameters: SiteParameters,
        resources: ResourceProvider,
        currencyFormatter: CurrencyFormatter,
        regularPrice: BigDecimal?,
        salePrice: BigDecimal?,
        isSaleScheduled: Boolean,
        saleStartDateGmt: Date?,
        saleEndDateGmt: Date?
    ): Map<String, String> {
        val pricingGroup = mutableMapOf<String, String>()
        if (regularPrice.isSet()) {
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

            // display product sale dates using the site's timezone, if available
            if (isSaleScheduled) {
                val dateOnSaleFrom = saleStartDateGmt?.offsetGmtDate(parameters.gmtOffset)
                val dateOnSaleTo = saleEndDateGmt?.offsetGmtDate(parameters.gmtOffset)

                val saleDates = when {
                    (dateOnSaleFrom != null && dateOnSaleTo != null) -> {
                        getProductSaleDates(dateOnSaleFrom, dateOnSaleTo, resources)
                    }
                    (dateOnSaleFrom != null && dateOnSaleTo == null) -> {
                        resources.getString(R.string.product_sale_date_from, dateOnSaleFrom.formatToMMMddYYYY())
                    }
                    (dateOnSaleFrom == null && dateOnSaleTo != null) -> {
                        resources.getString(R.string.product_sale_date_to, dateOnSaleTo.formatToMMMddYYYY())
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

    fun formatCurrencyOrNull(
        amount: BigDecimal?,
        currencyCode: String?,
        currencyFormatter: CurrencyFormatter
    ): String? {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        }
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
