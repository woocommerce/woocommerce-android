package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.utils.NullJsonAdapter
import java.math.BigDecimal

internal fun WooPushNotificationPreferences.toRequestMap(): Map<String, Any> = buildMap {
    storeOrder?.toRequest()?.takeIf { it.isNotEmpty() }?.let { put("store_order", it) }
    storeReview?.toRequestMap()?.takeIf { it.isNotEmpty() }?.let { put("store_review", it) }
    storeStock?.toRequestMap()?.takeIf { it.isNotEmpty() }?.let { put("store_stock", it) }
}

private fun WooPushNotificationPreferences.StoreOrderPreferences.toRequest() =
    StoreOrderPreferencesRequest(enabled, minAmount)

private fun WooPushNotificationPreferences.StoreReviewPreferences.toRequestMap(): Map<String, Any> = buildMap {
    enabled?.let { put("enabled", it) }
    maxRating?.let { put("max_rating", it) }
}

private fun WooPushNotificationPreferences.StoreStockPreferences.toRequestMap(): Map<String, Any> = buildMap {
    enabled?.let { put("enabled", it) }
    lowStock?.let { put("low_stock", it) }
    outOfStock?.let { put("out_of_stock", it) }
    onBackorder?.let { put("on_backorder", it) }
}

private data class StoreOrderPreferencesRequest(
    @SerializedName("enabled")
    val enabled: Boolean? = null,
    @SerializedName("min_amount")
    @JsonAdapter(NullJsonAdapter::class, nullSafe = false)
    val minAmount: BigDecimal? = null
) {
    fun isNotEmpty() = enabled != null || minAmount != null
}
