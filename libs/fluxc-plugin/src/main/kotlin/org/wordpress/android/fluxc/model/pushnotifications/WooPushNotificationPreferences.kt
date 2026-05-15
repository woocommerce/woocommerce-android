package org.wordpress.android.fluxc.model.pushnotifications

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class WooPushNotificationPreferences(
    @SerializedName("store_order")
    val storeOrder: StoreOrderPreferences? = null,
    @SerializedName("store_review")
    val storeReview: StoreReviewPreferences? = null,
    @SerializedName("store_stock")
    val storeStock: StoreStockPreferences? = null
) {
    data class StoreOrderPreferences(
        @SerializedName("enabled")
        val enabled: Boolean? = null,
        @SerializedName("min_amount")
        val minAmount: BigDecimal? = null
    )

    data class StoreReviewPreferences(
        @SerializedName("enabled")
        val enabled: Boolean? = null,
        @SerializedName("max_rating")
        val maxRating: Int? = null
    )

    data class StoreStockPreferences(
        @SerializedName("enabled")
        val enabled: Boolean? = null,
        @SerializedName("low_stock")
        val lowStock: Boolean? = null,
        @SerializedName("out_of_stock")
        val outOfStock: Boolean? = null,
        @SerializedName("on_backorder")
        val onBackorder: Boolean? = null
    )
}
