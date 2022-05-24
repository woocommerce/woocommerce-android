package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class CouponUtils @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) {
    fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return if (amount != null) {
            currencyCode?.let { currencyFormatter.formatCurrency(amount, it) }
                ?: amount.toString()
        } else {
            ""
        }
    }

    fun generateSummary(coupon: Coupon, currencyCode: String?): String {
        val amount = coupon.amount?.let { formatDiscount(it, coupon.type, currencyCode) }.orEmpty()
        val affectedArticles = formatAffectedArticles(
            coupon.productIds.size,
            coupon.restrictions.excludedProductIds.size,
            coupon.categoryIds.size,
            coupon.restrictions.excludedCategoryIds.size
        )
        return resourceProvider.getString(R.string.coupon_summary_template, amount, affectedArticles)
    }

    fun localizeType(couponType: Coupon.Type): String = when (couponType) {
        Coupon.Type.FixedCart -> resourceProvider.getString(R.string.coupon_type_fixed_cart)
        Coupon.Type.FixedProduct -> resourceProvider.getString(R.string.coupon_type_fixed_product)
        Coupon.Type.Percent -> resourceProvider.getString(R.string.coupon_type_percent)
        is Coupon.Type.Custom -> resourceProvider.getString(R.string.coupon_type_custom, couponType.value)
    }

    fun formatMinimumSpendingInfo(minimumAmount: BigDecimal?, currencyCode: String?): String? {
        if (minimumAmount == null || minimumAmount.isEqualTo(BigDecimal.ZERO)) return null
        return resourceProvider.getString(
            R.string.coupon_details_minimum_spend,
            formatCurrency(minimumAmount, currencyCode)
        )
    }

    fun formatMaximumSpendingInfo(maximumAmount: BigDecimal?, currencyCode: String?): String? {
        if (maximumAmount == null || maximumAmount.isEqualTo(BigDecimal.ZERO)) return null
        return resourceProvider.getString(
            R.string.coupon_details_maximum_spend,
            formatCurrency(maximumAmount, currencyCode)
        )
    }

    fun formatExpirationDate(expirationDate: Date): String {
        val dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG)
        return resourceProvider.getString(R.string.coupon_details_expiration_date, dateFormat.format(expirationDate))
    }

    fun formatUsageLimitPerUser(usageLimitPerUser: Int?) = usageLimitPerUser?.takeIf { it > 0 }?.let {
        StringUtils.getQuantityString(
            resourceProvider = resourceProvider,
            quantity = it,
            default = R.string.coupon_details_usage_limit_per_user_multiple,
            one = R.string.coupon_details_usage_limit_per_user_single
        )
    }

    fun formatUsageLimitPerCoupon(usageLimit: Int?) = usageLimit?.takeIf { it > 0 }?.let {
        StringUtils.getQuantityString(
            resourceProvider = resourceProvider,
            quantity = it,
            default = R.string.coupon_details_usage_limit_per_coupon_multiple,
            one = R.string.coupon_details_usage_limit_per_coupon_single
        )
    }

    fun formatUsageLimitPerItems(usageLimit: Int?) = usageLimit?.takeIf { it > 0 }?.let {
        StringUtils.getQuantityString(
            resourceProvider = resourceProvider,
            quantity = it,
            default = R.string.coupon_details_usage_limit_per_items_multiple,
            one = R.string.coupon_details_usage_limit_per_items_single
        )
    }

    fun formatRestrictedEmails(restrictedEmails: List<String>): String? {
        return restrictedEmails.takeIf { it.isNotEmpty() }?.let { emails ->
            resourceProvider.getString(R.string.coupon_details_restricted_emails, emails.joinToString(", "))
        }
    }

    /*
    - When only specific products or categories are defined: Display "x products" or "x categories"
    - When specific products/categories and exceptions are defined: Display "x products excl. y categories" etc.
    - When both specific products and categories are defined: Display "x products and y categories"
    - When only exceptions are defined: Display "everything excl. x products" or "everything excl. y categories"
     */
    private fun formatAffectedArticles(
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

    private fun formatDiscount(
        amount: BigDecimal,
        couponType: Coupon.Type?,
        currencyCode: String?
    ): String {
        return when (couponType) {
            Coupon.Type.Percent -> "${amount.toPlainString()}%"
            Coupon.Type.FixedCart, Coupon.Type.FixedProduct -> formatCurrency(amount, currencyCode)
            else -> amount.toPlainString()
        }
    }

    /*
    - If all products are included: "Apply 15% off to all products with the promo code ABCDE"
    - If only some products: "Apply 15% off to select products with the promo code ABCDE"
     */
    fun formatSharingMessage(
        amount: BigDecimal?,
        currencyCode: String?,
        couponCode: String?,
        includedProducts: Int,
        excludedProducts: Int
    ): String? {
        return if (amount != null && currencyCode != null && couponCode != null) {
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
            var params = ""
            if (amount == null) params += "`amount` "
            if (currencyCode == null) params += "`currencyCode` "
            if (couponCode == null) params += "`couponCode`"

            WooLog.e(
                WooLog.T.COUPONS,
                "Formatting coupon sharing message failed. null value found in $params"
            )

            null
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

    /**
     * Generate a random coupon code following the same logic as Core
     * https://github.com/woocommerce/woocommerce/blob/2e60d47a019a6e35f066f3ef43a56c0e761fc8e3/includes/admin/class-wc-admin-assets.php#L295
     */
    @Suppress("MagicNumber")
    fun generateRandomCode(): String {
        val availableCharacters = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray()
        val codeLength = 8
        return String(
            (1..codeLength).map {
                availableCharacters.random()
            }.toCharArray()
        )
    }
}
