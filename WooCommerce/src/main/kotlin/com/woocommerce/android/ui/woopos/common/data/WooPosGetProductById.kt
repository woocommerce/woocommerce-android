package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val store: WCProductStore,
    private val site: SelectedSite,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        store.getProductByRemoteId(site.getOrNull()!!, productId)?.toAppModel()
    }
}
