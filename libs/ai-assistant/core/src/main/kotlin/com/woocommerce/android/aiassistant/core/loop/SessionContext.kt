package com.woocommerce.android.aiassistant.core.loop

data class SessionContext(
    val siteId: Long,
    val catalogSnapshot: CatalogSnapshot,
)
