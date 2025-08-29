package org.wordpress.android.fluxc.store.pos.localcatalog

data class PosVariationsSyncResult(
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextPage: Int
)
