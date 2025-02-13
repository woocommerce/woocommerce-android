package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

data class CouponLine(
    val id: Long?,
    val code: String,
    val discount: String?,
    @SerializedName("discount_tax")
    val discountTax: String?,
)
