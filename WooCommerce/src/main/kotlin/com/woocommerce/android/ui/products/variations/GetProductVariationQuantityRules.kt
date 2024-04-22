package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCProductModel.QuantityRulesMetadataKeys.ALLOW_COMBINATION
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class GetProductVariationQuantityRules @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val variationDetailRepository: VariationDetailRepository,
    private val dispatchers: CoroutineDispatchers,
    private val productDetailRepository: ProductDetailRepository
) {
    suspend operator fun invoke(remoteProductId: Long, remoteVariationId: Long): QuantityRules? {
        return withContext(dispatchers.io) {
            val plugin = wooCommerceStore.getSitePlugin(
                site = selectedSite.get(),
                plugin = WooCommerceStore.WooPlugin.WOO_MIN_MAX_QUANTITIES
            )
            val isActive = plugin != null && plugin.isActive

            val isAllowCombinationDisabled =
                productDetailRepository.getProductMetadata(remoteProductId)?.let { metadata ->
                    metadata[ALLOW_COMBINATION] != "yes"
                } ?: true

            if (isActive && isAllowCombinationDisabled) {
                variationDetailRepository.getQuantityRules(remoteProductId, remoteVariationId)
            } else {
                null
            }
        }
    }
}
