package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences

internal fun WooPushNotificationPreferences.toRequestMap(): Map<String, Any> = buildMap {
    storeOrder?.toRequestMap()?.takeIf { it.isNotEmpty() }?.let { put("store_order", it) }
    storeReview?.toRequestMap()?.takeIf { it.isNotEmpty() }?.let { put("store_review", it) }
    storeStock?.toRequestMap()?.takeIf { it.isNotEmpty() }?.let { put("store_stock", it) }
}

private fun WooPushNotificationPreferences.StoreOrderPreferences.toRequestMap(): Map<String, Any> = buildMap {
    enabled?.let { put("enabled", it) }
    minAmount?.let { put("min_amount", it) }
}

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
