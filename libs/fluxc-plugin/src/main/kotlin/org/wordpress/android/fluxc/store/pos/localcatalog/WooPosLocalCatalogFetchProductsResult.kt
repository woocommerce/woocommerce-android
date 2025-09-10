package org.wordpress.android.fluxc.store.pos.localcatalog

import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductEntity

data class WooPosLocalCatalogFetchProductsResult(
    val products: List<WCPosProductEntity>,
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextOffset: Int,
    val totalPages: Int,
    val serverDate: String,
)
