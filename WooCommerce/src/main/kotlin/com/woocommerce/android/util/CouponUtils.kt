package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.viewmodel.ResourceProvider
import java.lang.StringBuilder
import java.math.BigDecimal
import javax.inject.Inject

class CouponUtils @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) {
    fun formatDiscount(
        amount: BigDecimal?,
        couponType: Coupon.Type?,
        currencyCode: String?
    ): String {
        return when (couponType) {
            Coupon.Type.Percent -> "$amount%"
            else -> formatCurrency(amount, currencyCode)
        }
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return if (amount != null) {
            currencyCode?.let { currencyFormatter.formatCurrency(amount, it) }
                ?: amount.toString()
        } else {
            ""
        }
    }

    /*
    - When only specific products or categories are defined: Display "x products" or "x categories"
    - When specific products/categories and exceptions are defined: Display "x products excl. y categories" etc.
    - When both specific products and categories are defined: Display "x products and y categories"
    - When only exceptions are defined: Display "everything excl. x products" or "everything excl. y categories"
     */
    fun formatAffectedArticles(
        includedProducts: Int,
        excludedProducts: Int,
        includedCategories: Int,
        excludedCategories: Int
    ): String {
        val included = when {
            includedProducts != 0 && includedCategories != 0 -> {
                resourceProvider.getString(
                    R.string.coupon_list_item_label_products_and_categories,
                    formatProducts(includedProducts),
                    formatCategories(includedCategories)
                )
            }
            includedProducts != 0 -> formatProducts(includedProducts)
            includedCategories != 0 -> formatCategories(includedCategories)
            else -> resourceProvider.getString(R.string.coupon_list_item_label_everything)
        }

        val excluded = when {
            excludedProducts != 0 && excludedCategories != 0 -> {
                resourceProvider.getString(
                    R.string.coupon_list_item_label_products_and_categories,
                    formatProducts(excludedProducts),
                    formatCategories(excludedCategories)
                )
            }
            excludedProducts != 0 -> formatProducts(excludedProducts)
            excludedCategories != 0 -> formatCategories(excludedCategories)
            else -> ""
        }

        return if (excluded.isNotEmpty()) {
            resourceProvider.getString(
                R.string.coupon_list_item_label_included_and_excluded,
                included,
                excluded
            )
        } else {
            included
        }
    }

    /*
    - If all products are included: "Apply 15% off to all products with the promo code ABCDE"
    - If only some products: "Apply 15% off to select products with the promo code ABCDE"
     */
    fun formatSharingMessage(
        amount: BigDecimal?,
        currencyCode: String?,
        couponCode: String,
        includedProducts: Int,
        excludedProducts: Int
    ): String {
        return if (amount != null && currencyCode != null) {
            if (includedProducts == 0 && excludedProducts == 0) {
                resourceProvider.getString(
                    R.string.coupon_details_share_coupon_all,
                    formatCurrency(amount, currencyCode),
                    couponCode
                )
            } else {
                resourceProvider.getString(
                    R.string.coupon_details_share_coupon_some,
                    formatCurrency(amount, currencyCode),
                    couponCode
                )
            }
        } else {
            ""
        }
    }

    private fun formatProducts(products: Int): String {
        return if (products > 0) {
            StringUtils.getQuantityString(
                resourceProvider,
                products,
                default = R.string.product_count_many,
                one = R.string.product_count_one
            )
        } else ""
    }

    private fun formatCategories(categories: Int): String {
        return if (categories > 0) {
            StringUtils.getQuantityString(
                resourceProvider,
                categories,
                default = R.string.category_count_many,
                one = R.string.category_count_one
            )
        } else ""
    }

    fun formatSpendingInfo(
        minimumAmount: BigDecimal?,
        maximumAmount: BigDecimal?,
        currencyCode: String?
    ): String {
        val sb = StringBuilder()

        if (minimumAmount != null) {
            sb.append(
                resourceProvider.getString(
                    R.string.coupon_details_minimum_spend,
                    formatCurrency(minimumAmount, currencyCode)
                )
            )
        }

        if (maximumAmount != null) {
            sb.append(
                resourceProvider.getString(
                    R.string.coupon_details_maximum_spend,
                    formatCurrency(maximumAmount, currencyCode)
                )
            )
        }

        return sb.toString()
    }
}
