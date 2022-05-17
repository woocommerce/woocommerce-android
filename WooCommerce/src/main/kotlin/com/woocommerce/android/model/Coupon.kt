package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.parseFromIso8601DateFormat
import com.woocommerce.android.extensions.parseGmtDateFromIso8601DateFormat
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
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
    val isShippingFree: Boolean? = null,
    val products: List<Product>,
    val categories: List<ProductCategory>,
    val restrictions: CouponRestrictions
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
            isShippingFree == otherCoupon.isShippingFree &&
            products == otherCoupon.products &&
            categories == otherCoupon.categories &&
            restrictions.isSameRestrictions(otherCoupon.restrictions)
    }

    @Parcelize
    class CouponRestrictions(
        val isForIndividualUse: Boolean? = null,
        val usageLimit: Int? = null,
        val usageLimitPerUser: Int? = null,
        val limitUsageToXItems: Int? = null,
        val areSaleItemsExcluded: Boolean? = null,
        val minimumAmount: BigDecimal? = null,
        val maximumAmount: BigDecimal? = null,
        val excludedProducts: List<Product>,
        val excludedCategories: List<ProductCategory>,
        val restrictedEmails: List<String>
    ) : Parcelable {
        fun isSameRestrictions(otherRestrictions: CouponRestrictions): Boolean {
            return isForIndividualUse == otherRestrictions.isForIndividualUse &&
                usageLimit == otherRestrictions.usageLimit &&
                usageLimitPerUser == otherRestrictions.usageLimitPerUser &&
                limitUsageToXItems == otherRestrictions.limitUsageToXItems &&
                areSaleItemsExcluded == otherRestrictions.areSaleItemsExcluded &&
                minimumAmount == otherRestrictions.minimumAmount &&
                maximumAmount == otherRestrictions.maximumAmount &&
                excludedProducts == otherRestrictions.excludedProducts &&
                excludedCategories == otherRestrictions.excludedCategories &&
                restrictedEmails == otherRestrictions.restrictedEmails
        }
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

fun CouponDataModel.toAppModel() = Coupon(
    id = coupon.id,
    code = coupon.code,
    amount = coupon.amount,
    dateCreatedGmt = coupon.dateCreatedGmt.parseGmtDateFromIso8601DateFormat(),
    dateModifiedGmt = coupon.dateModifiedGmt.parseGmtDateFromIso8601DateFormat(),
    type = coupon.discountType?.let { Coupon.Type.fromDataModel(it) },
    description = coupon.description,
    dateExpires = coupon.dateExpiresGmt.parseFromIso8601DateFormat(),
    usageCount = coupon.usageCount,
    isShippingFree = coupon.isShippingFree,
    products = products.map { it.toAppModel() },
    categories = categories.map { it.toAppModel() },
    restrictions = Coupon.CouponRestrictions(
        isForIndividualUse = coupon.isForIndividualUse,
        usageLimit = coupon.usageLimit,
        usageLimitPerUser = coupon.usageLimitPerUser,
        limitUsageToXItems = coupon.limitUsageToXItems,
        areSaleItemsExcluded = coupon.areSaleItemsExcluded,
        minimumAmount = coupon.minimumAmount,
        maximumAmount = coupon.maximumAmount,
        excludedProducts = excludedProducts.map { it.toAppModel() },
        excludedCategories = excludedCategories.map { it.toAppModel() },
        restrictedEmails = restrictedEmails.map { it.email },
    )
)
