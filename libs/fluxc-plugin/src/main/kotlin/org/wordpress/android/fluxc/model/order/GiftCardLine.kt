package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

data class GiftCardLine(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("code")
    val code: String?,
    @SerializedName("amount")
    val amount: String?,
)
