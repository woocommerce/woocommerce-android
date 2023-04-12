package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class RefreshBundledProducts @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productId: Long) {
        withContext(dispatchers.io) {
            val siteModel = selectedSite.get()
            val bundledProducts = productStore.observeBundledProducts(siteModel, productId).first()
            val remoteIds = bundledProducts.map { it.bundledProductId.value }.distinct()
            productStore.fetchProductListSynced(siteModel, remoteIds)
        }
    }
}
