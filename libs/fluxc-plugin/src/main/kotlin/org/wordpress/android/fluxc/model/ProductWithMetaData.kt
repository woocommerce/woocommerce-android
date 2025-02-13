package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.metadata.WCMetaData

data class ProductWithMetaData(
    val product: WCProductModel,
    val metaData: List<WCMetaData> = emptyList()
)
