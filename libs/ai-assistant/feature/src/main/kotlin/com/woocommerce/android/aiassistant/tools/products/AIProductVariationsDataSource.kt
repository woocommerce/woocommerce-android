package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

internal class AIProductVariationsDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
) {

    suspend fun fetchVariations(
        productId: Long,
        page: Int = 1,
        perPage: Int = PAGE_SIZE,
    ): Result<List<WCProductVariationModel>> {
        val site = selectedSite.get()
        val clampedPage = page.coerceAtLeast(1)
        val clampedPerPage = perPage.coerceIn(1, MAX_PAGE_SIZE)
        val payload = WCProductStore.FetchProductVariationsPayload(
            site = site,
            remoteProductId = productId,
            pageSize = clampedPerPage,
            offset = (clampedPage - 1) * clampedPerPage,
        )
        val result = productStore.fetchProductVariations(payload)

        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            Result.success(requireNotNull(result.model).variations)
        }
    }

    suspend fun getVariation(productId: Long, variationId: Long): Result<WCProductVariationModel> {
        val site = selectedSite.get()
        val cached = productStore.getVariationByRemoteId(site, productId, variationId)
        if (cached != null) return Result.success(cached)

        val event = productStore.fetchSingleVariation(site, productId, variationId)
        return if (event.isError) {
            Result.failure(OnChangedException(requireNotNull(event.error)))
        } else {
            val variation = productStore.getVariationByRemoteId(site, productId, variationId)
            variation?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Variation $variationId not found after fetch"))
        }
    }

    private companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}
