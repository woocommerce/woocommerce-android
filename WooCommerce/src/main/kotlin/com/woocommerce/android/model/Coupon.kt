package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.parseFromIso8601DateFormat
import com.woocommerce.android.extensions.parseGmtDateFromIso8601DateFormat
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Coupon(
    val id: Long,
    val code: String? = null,
    val amount: BigDecimal? = null,
    val dateCreatedGmt: Date? = null,
    val dateModifiedGmt: Date? = null,
    val type: Type? = null,
    val description: String? = null,
    val dateExpires: Date? = null,
    val usageCount: Int? = null,
    val isForIndividualUse: Boolean? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val isShippingFree: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val minimumAmount: BigDecimal? = null,
    val maximumAmount: BigDecimal? = null,
    val productIds: List<Long>,
    val excludedProductIds: List<Long>,
    val categoryIds: List<Long>,
    val excludedCategoryIds: List<Long>,
    val restrictedEmails: List<String>
) : Parcelable {
    @Suppress("ComplexMethod")
    fun isSameCoupon(otherCoupon: Coupon): Boolean {
        return id == otherCoupon.id &&
            code == otherCoupon.code &&
            amount isEqualTo otherCoupon.amount &&
            type == otherCoupon.type &&
            description == otherCoupon.description &&
            dateExpires == otherCoupon.dateExpires &&
            usageCount == otherCoupon.usageCount &&
            isForIndividualUse == otherCoupon.isForIndividualUse &&
            usageLimit == otherCoupon.usageLimit &&
            usageLimitPerUser == otherCoupon.usageLimitPerUser &&
            limitUsageToXItems == otherCoupon.limitUsageToXItems &&
            isShippingFree == otherCoupon.isShippingFree &&
            areSaleItemsExcluded == otherCoupon.areSaleItemsExcluded &&
            minimumAmount == otherCoupon.minimumAmount &&
            maximumAmount == otherCoupon.maximumAmount &&
            productIds == otherCoupon.productIds &&
            excludedProductIds == otherCoupon.excludedProductIds &&
            categoryIds == otherCoupon.categoryIds &&
            excludedCategoryIds == otherCoupon.excludedCategoryIds &&
            restrictedEmails == otherCoupon.restrictedEmails
    }

    sealed class Type(open val value: String) : Parcelable {
        companion object {
            fun fromDataModel(dataType: CouponEntity.DiscountType): Type {
                return when (dataType) {
                    CouponEntity.DiscountType.Percent -> Percent
                    CouponEntity.DiscountType.FixedCart -> FixedCart
                    CouponEntity.DiscountType.FixedProduct -> FixedProduct
                    is CouponEntity.DiscountType.Custom -> Custom(dataType.value)
                }
            }
        }

        @Parcelize
        object Percent : Type(CouponEntity.DiscountType.Percent.value)

        @Parcelize
        object FixedCart : Type(CouponEntity.DiscountType.FixedCart.value)

        @Parcelize
        object FixedProduct : Type(CouponEntity.DiscountType.FixedProduct.value)

        @Parcelize
        data class Custom(override val value: String) : Type(value)
    }
}

fun CouponWithEmails.toAppModel() = Coupon(
    id = coupon.id,
    code = coupon.code,
    amount = coupon.amount,
    dateCreatedGmt = coupon.dateCreatedGmt.parseGmtDateFromIso8601DateFormat(),
    dateModifiedGmt = coupon.dateModifiedGmt.parseGmtDateFromIso8601DateFormat(),
    type = coupon.discountType?.let { Coupon.Type.fromDataModel(it) },
    description = coupon.description,
    dateExpires = coupon.dateExpiresGmt.parseFromIso8601DateFormat(),
    usageCount = coupon.usageCount,
    isForIndividualUse = coupon.isForIndividualUse,
    usageLimit = coupon.usageLimit,
    usageLimitPerUser = coupon.usageLimitPerUser,
    limitUsageToXItems = coupon.limitUsageToXItems,
    isShippingFree = coupon.isShippingFree,
    areSaleItemsExcluded = coupon.areSaleItemsExcluded,
    minimumAmount = coupon.minimumAmount,
    maximumAmount = coupon.maximumAmount,
    productIds = coupon.includedProductIds.orEmpty(),
    excludedProductIds = coupon.excludedProductIds.orEmpty(),
    categoryIds = coupon.includedCategoryIds.orEmpty(),
    excludedCategoryIds = coupon.excludedCategoryIds.orEmpty(),
    restrictedEmails = restrictedEmails.map { it.email }
)
