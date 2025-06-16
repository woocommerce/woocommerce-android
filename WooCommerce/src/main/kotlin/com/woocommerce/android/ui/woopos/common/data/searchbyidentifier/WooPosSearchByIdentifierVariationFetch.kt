package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierVariationFetch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val variationsCache: WooPosVariationsLRUCache
) {
    sealed class VariationFetchResult {
        data class Success(val variation: ProductVariation) : VariationFetchResult()
        data object NetworkError : VariationFetchResult()
        data object NotFound : VariationFetchResult()
    }

    @Suppress("ReturnCount")
    suspend operator fun invoke(variationId: Long, parentId: Long): VariationFetchResult {
        val variationResult = productStore.fetchSingleVariation(
            selectedSite.get(),
            parentId,
            variationId
        )

        if (variationResult.isError) {
            return VariationFetchResult.NetworkError
        }

        val variation = productStore.getVariationByRemoteId(
            selectedSite.get(),
            parentId,
            variationId
        )?.toAppModel()

        return if (variation != null) {
            variationsCache.add(parentId, variation)
            VariationFetchResult.Success(variation)
        } else {
            VariationFetchResult.NotFound
        }
    }
}
