package com.woocommerce.android.ui.woopos.settings.details.localcatalog

data class WooPosSettingsLocalCatalogState(
    val catalogStatus: CatalogStatus = CatalogStatus.Loading,
    val cellularCapability: CellularCapability = CellularCapability.None,
) {
    sealed class CatalogStatus {
        data class Available(
            val productCount: Int,
            val variationCount: Int,
            val lastUpdate: String,
            val lastFullUpdate: String
        ) : CatalogStatus()

        object Loading : CatalogStatus()
        object RefreshingCatalog : CatalogStatus()
    }

    sealed class CellularCapability {
        object None : CellularCapability()
        data class Available(val allowCellularDataUpdate: Boolean) : CellularCapability()
    }
}
