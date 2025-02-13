package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.converters.DiscountTypeConverter
import java.math.BigDecimal

@Entity(
    tableName = "Coupons",
    primaryKeys = ["id", "localSiteId"]
)
data class CouponEntity(
    val id: RemoteId,
    val localSiteId: LocalId,
    val code: String? = null,
    val amount: BigDecimal? = null,
    val dateCreated: String? = null,
    val dateCreatedGmt: String? = null,
    val dateModified: String? = null,
    val dateModifiedGmt: String? = null,
    @field:TypeConverters(DiscountTypeConverter::class) val discountType: DiscountType? = null,
    val description: String? = null,
    val dateExpires: String? = null,
    val dateExpiresGmt: String? = null,
    val usageCount: Int? = null,
    val isForIndividualUse: Boolean? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val isShippingFree: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val minimumAmount: BigDecimal? = null,
    val maximumAmount: BigDecimal? = null,
    val includedProductIds: List<Long>? = null,
    val excludedProductIds: List<Long>? = null,
    val includedCategoryIds: List<Long>? = null,
    val excludedCategoryIds: List<Long>? = null
) {
    sealed class DiscountType(open val value: String) {
        object Percent : DiscountType("percent")
        object FixedCart : DiscountType("fixed_cart")
        object FixedProduct : DiscountType("fixed_product")
        data class Custom(override val value: String) : DiscountType(value)

        companion object {
            fun fromString(value: String): DiscountType {
                return when (value) {
                    Percent.value -> Percent
                    FixedProduct.value -> FixedProduct
                    FixedCart.value -> FixedCart
                    else -> Custom(value)
                }
            }
        }

        override fun toString() = value
    }
}

data class CouponWithEmails(
    @Embedded val coupon: CouponEntity,
    @Relation(parentColumn = "id", entityColumn = "couponId")
    val restrictedEmails: List<CouponEmailEntity>
)
