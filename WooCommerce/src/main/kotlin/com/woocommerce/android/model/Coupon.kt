package com.woocommerce.android.model

import com.woocommerce.android.extensions.parseFromIso8601DateFormat
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import java.math.BigDecimal
import java.util.*

data class Coupon(
    val id: Long,
    val siteId: Long,
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
    val excludedCategories: List<ProductCategory>
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
    id = couponEntity.id,
    siteId = couponEntity.siteId,
    code = couponEntity.code,
    amount = couponEntity.amount?.toBigDecimalOrNull(),
    dateCreatedGmt = couponEntity.dateCreatedGmt.parseFromIso8601DateFormat(),
    dateModifiedGmt = couponEntity.dateModifiedGmt.parseFromIso8601DateFormat(),
    type = couponEntity.discountType?.let { Coupon.Type.fromString(it) },
    description = couponEntity.description,
    dateExpiresGmt = couponEntity.dateExpiresGmt.parseFromIso8601DateFormat(),
    usageCount = couponEntity.usageCount,
    isForIndividualUse = couponEntity.isForIndividualUse,
    usageLimit = couponEntity.usageLimit,
    usageLimitPerUser = couponEntity.usageLimitPerUser,
    limitUsageToXItems = couponEntity.limitUsageToXItems,
    isShippingFree = couponEntity.isShippingFree,
    areSaleItemsExcluded = couponEntity.areSaleItemsExcluded,
    minimumAmount = couponEntity.minimumAmount?.toBigDecimalOrNull(),
    maximumAmount = couponEntity.maximumAmount?.toBigDecimalOrNull(),
    products = products.map { it.toAppModel() },
    excludedProducts = excludedProducts.map { it.toAppModel() },
    categories = categories.map { it.toAppModel() },
    excludedCategories = excludedCategories.map { it.toAppModel() }
)
