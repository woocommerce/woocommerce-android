package com.woocommerce.android.ui.woopos.settings.details.localcatalog

data class WooPosSettingsLocalCatalogState(
    val catalogStatus: CatalogStatus = CatalogStatus.LoadingStatus,
    val allowCellularDataUpdate: Boolean = false,
) {
    sealed class CatalogStatus {
        data class Available(
            val catalogSize: String,
            val lastUpdate: String,
            val lastFullUpdate: String
        ) : CatalogStatus()

        object LoadingStatus : CatalogStatus()
        object RefreshingCatalog : CatalogStatus()
    }
}
