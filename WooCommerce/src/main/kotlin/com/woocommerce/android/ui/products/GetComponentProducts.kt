package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Component
import com.woocommerce.android.model.ComponentMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GetComponentProducts @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productId: Long): List<Component> {
        return withContext(dispatchers.io) {
            val siteModel = selectedSite.get()
            productStore.getCompositeProducts(siteModel, productId).map {
                ComponentMapper.toAppModel(it)
            }
        }
    }
}
