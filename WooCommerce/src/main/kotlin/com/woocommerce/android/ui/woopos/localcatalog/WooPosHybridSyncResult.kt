package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

sealed class WooPosHybridSyncResult : WooPosSyncResult()

sealed class ProductsResult : WooPosHybridSyncResult() {
    data class Cached(val products: List<WooPosProductModel>) : ProductsResult()
    data class Remote(val productsResult: Result<List<WooPosProductModel>>) : ProductsResult()
}

sealed class VariationsResult : WooPosHybridSyncResult() {
    data class Cached(val data: List<WooPosVariation>) : VariationsResult()
    data class Remote(val result: Result<List<WooPosVariation>>) : VariationsResult()
}
