package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GetProductsByIds @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productRemoteIds: List<Long>): List<Product> {
        return withContext(dispatchers.io) {
            val siteModel = selectedSite.get()
            productStore.fetchProductListSynced(siteModel, productRemoteIds)
                ?.map { it.toAppModel() }
                ?: emptyList()
        }
    }
}
