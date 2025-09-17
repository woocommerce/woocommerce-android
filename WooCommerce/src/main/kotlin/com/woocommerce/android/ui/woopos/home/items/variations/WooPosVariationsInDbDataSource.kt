package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosVariationsInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val mapper: WooPosVariationMapper
) : WooPosVariationsDataSourceInterface {
    private fun getVariationsFromDatabaseFlow(productId: Long): Flow<List<WooPosVariation>> {
        val siteModel = selectedSite.getOrNull() ?: return flow { emit(emptyList()) }

        return posLocalCatalogStore.observeVariationsForProduct(siteModel.id, productId)
            .map { result ->
                result.getOrNull()?.map { it.toWooPosVariation(mapper) } ?: emptyList()
            }
    }

    override suspend fun resetState() = Unit

    override fun canLoadMore(numOfVariations: Int): Boolean = false

    override fun fetchFirstPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<FetchResult> = getVariationsFromDatabaseFlow(productId)
        .map { variations ->
            FetchResult.Remote(Result.success(variations.applyFilter()))
        }
        .flowOn(Dispatchers.IO)

    override suspend fun loadMore(productId: Long): Result<List<WooPosVariation>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }
}

private fun List<WooPosVariation>.applyFilter(): List<WooPosVariation> {
    return filter { !it.isDownloadable }
}
