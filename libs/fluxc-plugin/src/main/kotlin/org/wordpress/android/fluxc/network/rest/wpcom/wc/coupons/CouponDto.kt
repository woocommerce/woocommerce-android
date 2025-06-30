package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType

data class CouponDto(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String?,
    @SerializedName("amount") val amount: String?,
    @SerializedName("date_created") val dateCreated: String?,
    @SerializedName("date_created_gmt") val dateCreatedGmt: String?,
    @SerializedName("date_modified") val dateModified: String?,
    @SerializedName("date_modified_gmt") val dateModifiedGmt: String?,
    @SerializedName("discount_type") val discountType: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("date_expires") val dateExpires: String?,
    @SerializedName("date_expires_gmt") val dateExpiresGmt: String?,
    @SerializedName("usage_count") val usageCount: Int?,
    @SerializedName("individual_use") val isForIndividualUse: Boolean?,
    @SerializedName("product_ids") val productIds: List<Long>?,
    @SerializedName("excluded_product_ids") val excludedProductIds: List<Long>?,
    @SerializedName("usage_limit") val usageLimit: Int?,
    @SerializedName("usage_limit_per_user") val usageLimitPerUser: Int?,
    @SerializedName("limit_usage_to_x_items") val limitUsageToXItems: Int?,
    @SerializedName("free_shipping") val isShippingFree: Boolean?,
    @SerializedName("product_categories") val productCategoryIds: List<Long>?,
    @SerializedName("excluded_product_categories") val excludedProductCategoryIds: List<Long>?,
    @SerializedName("exclude_sale_items") val areSaleItemsExcluded: Boolean?,
    @SerializedName("minimum_amount") val minimumAmount: String?,
    @SerializedName("maximum_amount") val maximumAmount: String?,
    @SerializedName("email_restrictions") val restrictedEmails: List<String>?,
    @SerializedName("used_by") val usedBy: List<String>?
) {
    fun toDataModel(localSiteId: LocalId): CouponEntity =
        CouponEntity(
            id = RemoteId(id),
            localSiteId = localSiteId,
            code = code,
            amount = amount?.toBigDecimalOrNull(),
            dateCreated = dateCreated,
            dateCreatedGmt = dateCreatedGmt,
            dateModified = dateModified,
            dateModifiedGmt = dateModifiedGmt,
            discountType = discountType?.let { DiscountType.fromString(it) },
            description = description,
            dateExpires = dateExpires,
            dateExpiresGmt = dateExpiresGmt,
            usageCount = usageCount,
            isForIndividualUse = isForIndividualUse,
            usageLimit = usageLimit,
            usageLimitPerUser = usageLimitPerUser,
            limitUsageToXItems = limitUsageToXItems,
            isShippingFree = isShippingFree,
            areSaleItemsExcluded = areSaleItemsExcluded,
            minimumAmount = minimumAmount?.toBigDecimalOrNull(),
            maximumAmount = maximumAmount?.toBigDecimalOrNull(),
            includedProductIds = productIds,
            excludedProductIds = excludedProductIds,
            includedCategoryIds = productCategoryIds,
            excludedCategoryIds = excludedProductCategoryIds
        )
}
