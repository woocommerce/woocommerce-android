package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierVariationFetcher @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val variationsCache: WooPosVariationsLRUCache
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(variationId: Long, parentId: Long): ProductVariation? {
        val cachedVariation = variationsCache.get(variationId)?.find { it.remoteVariationId == variationId }

        if (cachedVariation != null) {
            return cachedVariation
        }
        
        val variationResult = productStore.fetchSingleVariation(
            selectedSite.get(),
            parentId,
            variationId
        )

        if (variationResult.isError) {
            return null
        }

        return productStore.getVariationByRemoteId(
            selectedSite.get(),
            parentId,
            variationId
        )?.toAppModel()
            ?.also {
                variationsCache.add(parentId, it)
            }
    }
}