package org.wordpress.android.fluxc.store.pos.localcatalog

data class WooPosLocalCatalogSyncResult(
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextOffset: Int,
    val totalPages: Int,
)
