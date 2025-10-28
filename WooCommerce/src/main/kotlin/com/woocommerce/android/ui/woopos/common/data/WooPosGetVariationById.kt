package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import javax.inject.Inject

class WooPosGetVariationById @Inject constructor(private val dataSource: WooPosProductsDataSource) {
    suspend operator fun invoke(productId: Long, variationId: Long): WooPosVariation? =
        dataSource.getVariationById(productId, variationId)
}
