package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val dataSource: WooPosProductsDataSource,
) {
    suspend operator fun invoke(productId: Long): WooPosProductModel? = withContext(IO) {
        dataSource.getProductById(LocalOrRemoteId.RemoteId(productId))
    }
}
