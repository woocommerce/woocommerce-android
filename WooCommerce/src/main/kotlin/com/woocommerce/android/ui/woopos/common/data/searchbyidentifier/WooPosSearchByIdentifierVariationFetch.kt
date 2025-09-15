package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierVariationFetch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val variationsCache: WooPosVariationsLRUCache,
    private val errorMapper: WooPosSearchByIdentifierProductErrorMapper,
    private val mapper: WooPosVariationMapper
) {
    sealed class VariationFetchResult {
        data class Success(val variation: WooPosVariation) : VariationFetchResult()
        data class Failure(val error: WooPosSearchByIdentifierResult.Error) : VariationFetchResult()
    }

    @Suppress("ReturnCount")
    suspend operator fun invoke(variationId: Long, parentId: Long): VariationFetchResult {
        val variationResult = productStore.fetchSingleVariation(
            selectedSite.get(),
            parentId,
            variationId
        )

        if (variationResult.isError) {
            return VariationFetchResult.Failure(errorMapper(variationResult.error))
        }

        val variation = productStore.getVariationByRemoteId(
            selectedSite.get(),
            parentId,
            variationId
        )?.toWooPosVariation(mapper)

        return if (variation != null) {
            variationsCache.add(parentId, variation)
            VariationFetchResult.Success(variation)
        } else {
            VariationFetchResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError(
                    "Variation not found for ID: $variationId"
                )
            )
        }
    }
}
