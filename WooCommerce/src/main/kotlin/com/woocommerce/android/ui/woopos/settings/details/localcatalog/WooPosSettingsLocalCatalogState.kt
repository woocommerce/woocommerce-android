package com.woocommerce.android.ui.woopos.settings.details.localcatalog

data class WooPosSettingsLocalCatalogState(
    val catalogStatus: CatalogStatus? = null,
    val allowCellularDataUpdate: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

data class CatalogStatus(
    val catalogSize: String,
    val lastUpdate: String,
    val lastFullUpdate: String
)
