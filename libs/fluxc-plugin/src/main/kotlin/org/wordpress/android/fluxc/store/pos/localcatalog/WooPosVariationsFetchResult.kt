package org.wordpress.android.fluxc.store.pos.localcatalog

import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity

data class WooPosVariationsFetchResult(
    val variations: List<WooPosVariationEntity>,
    override val syncedCount: Int,
    override val hasMore: Boolean,
    override val nextPage: Int,
    override val totalPages: Int,
    override val serverDate: String,
) : WooPosPaginatedFetchResult<WooPosVariationEntity> {
    override val items: List<WooPosVariationEntity> get() = variations
}
