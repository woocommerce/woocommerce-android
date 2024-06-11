package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class GetProductQuantityRules @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val productDetailRepository: ProductDetailRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(productId: Long): QuantityRules? {
        return withContext(dispatchers.io) {
            val plugin = wooCommerceStore.getSitePlugin(
                site = selectedSite.get(),
                plugin = WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES
            )
            val isActive = plugin != null && plugin.isActive

            if (isActive) {
                productDetailRepository.getQuantityRules(productId)
            } else {
                null
            }
        }
    }
}
