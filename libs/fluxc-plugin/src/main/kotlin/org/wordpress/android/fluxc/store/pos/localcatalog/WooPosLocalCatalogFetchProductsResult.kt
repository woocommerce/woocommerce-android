package org.wordpress.android.fluxc.store.pos.localcatalog

import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity

data class WooPosLocalCatalogFetchProductsResult(
    val products: List<WooPosProductEntity>,
    override val syncedCount: Int,
    override val hasMore: Boolean,
    override val nextPage: Int,
    override val totalPages: Int,
    override val serverDate: String,
) : WooPosPaginatedFetchResult<WooPosProductEntity> {
    override val items: List<WooPosProductEntity> get() = products
}
