package org.wordpress.android.fluxc.store.pos.localcatalog

import org.wordpress.android.fluxc.persistence.entity.pos.WCPosVariationModel

data class WooPosVariationsFetchResult(
    val variations: List<WCPosVariationModel>,
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextPage: Int,
    val totalPages: Int,
    val serverDate: String,
)
