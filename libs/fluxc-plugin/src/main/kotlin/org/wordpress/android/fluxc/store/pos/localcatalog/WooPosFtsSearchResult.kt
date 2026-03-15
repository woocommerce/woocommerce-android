package org.wordpress.android.fluxc.store.pos.localcatalog

import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity

sealed class WooPosFtsSearchResult {
    data class Product(val entity: WooPosProductEntity) : WooPosFtsSearchResult()
    data class Variation(
        val entity: WooPosVariationEntity,
        val parentProductName: String,
    ) : WooPosFtsSearchResult()
}
