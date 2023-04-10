package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GetProductByIds @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(productIds: List<Long>): List<Product> {
        return withContext(dispatchers.io) {
            delay(1000)
            productStore.getProductsByRemoteIds(selectedSite.get(), productIds).map { it.toAppModel() }
        }
    }
}
