package com.woocommerce.android.ui.products

import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GetCategoriesByIds @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productRemoteIds: List<Long>): List<ProductCategory> {
        return withContext(dispatchers.io) {
            val siteModel = selectedSite.get()
            productStore.fetchProductCategoryListSynced(siteModel, productRemoteIds)
                ?.map { it.toProductCategory() }
                ?: emptyList()
        }
    }
}
