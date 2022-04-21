package com.woocommerce.android.model

import com.woocommerce.android.extensions.parseGMTDateFromIso8601DateFormat
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import java.math.BigDecimal
import java.util.Date

data class Coupon(
    val id: Long,
    val code: String? = null,
    val amount: BigDecimal? = null,
    val dateCreatedGmt: Date? = null,
    val dateModifiedGmt: Date? = null,
    val type: Type? = null,
    val description: String? = null,
    val dateExpiresGmt: Date? = null,
    val usageCount: Int? = null,
    val isForIndividualUse: Boolean? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val isShippingFree: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val minimumAmount: BigDecimal? = null,
    val maximumAmount: BigDecimal? = null,
    val products: List<Product>,
    val excludedProducts: List<Product>,
    val categories: List<ProductCategory>,
    val excludedCategories: List<ProductCategory>,
    val restrictedEmails: List<String>
) {
    sealed class Type(open val value: String) {
        companion object {
            const val PERCENT = "percent"
            const val FIXED_CART = "fixed_cart"
            const val FIXED_PRODUCT = "fixed_product"

            fun fromString(value: String): Type {
                return when (value) {
                    PERCENT -> Percent
                    FIXED_CART -> FixedCart
                    FIXED_PRODUCT -> FixedProduct
                    else -> Custom(value)
                }
            }
        }
        object Percent : Type(PERCENT)
        object FixedCart : Type(FIXED_CART)
        object FixedProduct : Type(FIXED_PRODUCT)
        data class Custom(override val value: String) : Type(value)
    }
}

fun CouponDataModel.toAppModel() = Coupon(
    id = coupon.id,
    code = coupon.code,
    amount = coupon.amount?.toBigDecimalOrNull(),
    dateCreatedGmt = coupon.dateCreatedGmt.parseGMTDateFromIso8601DateFormat(),
    dateModifiedGmt = coupon.dateModifiedGmt.parseGMTDateFromIso8601DateFormat(),
    type = coupon.discountType?.let { Coupon.Type.fromString(it) },
    description = coupon.description,
    dateExpiresGmt = coupon.dateExpiresGmt.parseGMTDateFromIso8601DateFormat(),
    usageCount = coupon.usageCount,
    isForIndividualUse = coupon.isForIndividualUse,
    usageLimit = coupon.usageLimit,
    usageLimitPerUser = coupon.usageLimitPerUser,
    limitUsageToXItems = coupon.limitUsageToXItems,
    isShippingFree = coupon.isShippingFree,
    areSaleItemsExcluded = coupon.areSaleItemsExcluded,
    minimumAmount = coupon.minimumAmount?.toBigDecimalOrNull(),
    maximumAmount = coupon.maximumAmount?.toBigDecimalOrNull(),
    products = products.map { it.toAppModel() },
    excludedProducts = excludedProducts.map { it.toAppModel() },
    categories = categories.map { it.toAppModel() },
    excludedCategories = excludedCategories.map { it.toAppModel() },
    restrictedEmails = restrictedEmails.map { it.email }
)
