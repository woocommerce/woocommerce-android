package org.wordpress.android.fluxc.store.pos.localcatalog

data class WooPosVariationsSyncResult(
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextPage: Int,
    val totalPages: Int,
    val serverDate: String,
)
