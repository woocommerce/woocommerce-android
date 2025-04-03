package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.items.search.WooPosSearchProductsMockedDataSource
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val store: WCProductStore,
    private val site: SelectedSite,
    private val isWooPosSearchEnabled: WooPosIsProductsSearchEnabled,
    private val searchProductsMockedDataSource: WooPosSearchProductsMockedDataSource,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        val result = store.getProductByRemoteId(site.getOrNull()!!, productId)?.toAppModel()
        if (result == null && isWooPosSearchEnabled()) {
            searchProductsMockedDataSource.getProductById(productId)
        } else {
            result
        }
    }
}
