package org.wordpress.android.fluxc.store.pos.localcatalog

data class WooPosGenerateCatalogResult(
    val scheduledAt: String? = null,
    val completedAt: String? = null,
    val state: String? = null,
    val progress: Int? = null,
    val processed: Int? = null,
    val total: Int? = null,
    val url: String? = null,
    val productFields: List<String>? = null,
    val variationFields: List<String>? = null,
)
