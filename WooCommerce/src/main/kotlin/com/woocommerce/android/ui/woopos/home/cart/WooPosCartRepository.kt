package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCartRepository @Inject constructor(
    private val store: WCProductStore,
    private val site: SelectedSite,
) {
    suspend fun getProductById(productId: Long): Product? = withContext(IO) {
        store.getProductByRemoteId(site.getOrNull()!!, productId)?.toAppModel()
    }
}
