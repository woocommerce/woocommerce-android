package org.wordpress.android.fluxc.model.coupon

data class UpdateCouponRequest(
    val code: String? = null,
    val amount: String? = null,
    val discountType: String? = null,
    val description: String? = null,
    val expiryDate: String? = null,
    val minimumAmount: String? = null,
    val maximumAmount: String? = null,
    val productIds: List<Long>? = null,
    val excludedProductIds: List<Long>? = null,
    val productCategoryIds: List<Long>? = null,
    val excludedProductCategoryIds: List<Long>? = null,
    val isShippingFree: Boolean? = null,
    val isForIndividualUse: Boolean? = null,
    val areSaleItemsExcluded: Boolean? = null,
    val usageLimit: Int? = null,
    val usageLimitPerUser: Int? = null,
    val limitUsageToXItems: Int? = null,
    val restrictedEmails: List<String>? = null
)
