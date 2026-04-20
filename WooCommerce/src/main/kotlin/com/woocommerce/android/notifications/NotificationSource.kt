package com.woocommerce.android.notifications

enum class NotificationSource(val trackingValue: String) {
    WPCOM("wpcom"),
    WOO_DRIVEN("woo_driven")
}

/**
 * Builds the stable `<site-or-store>:<type>:<entity-id>` analytics id for a Woo-driven
 * notification, or `null` when the type has no segment (Blaze, local reminder), the entity id
 * is zero, or no site/store fallback is available.
 */
fun buildWooDrivenAnalyticsId(
    remoteSiteId: Long,
    uniqueId: Long,
    wooTypeSegment: String?,
    storeIdFallback: String?
): String? {
    val siteSegment = when {
        remoteSiteId != 0L -> remoteSiteId.toString()
        !storeIdFallback.isNullOrEmpty() -> storeIdFallback
        else -> null
    }
    return when {
        wooTypeSegment == null || siteSegment == null || uniqueId == 0L -> null
        else -> "$siteSegment:$wooTypeSegment:$uniqueId"
    }
}
