package com.woocommerce.android.aiassistant.tools

internal data class CachedLookupResult<T>(
    val items: List<T>,
    val cacheHitCount: Int,
    val cacheMissCount: Int,
    val fetchAttempted: Boolean,
    val fetchFailed: Boolean,
)
