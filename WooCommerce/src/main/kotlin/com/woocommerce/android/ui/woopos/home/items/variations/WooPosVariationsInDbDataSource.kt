package com.woocommerce.android.ui.woopos.home.items.variations

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosVariationsInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val mapper: WooPosVariationMapper
) : WooPosVariationsDataSourceInterface {

    private suspend fun getVariationsFromDatabase(productId: Long): List<WooPosVariation> {
        val siteModel = selectedSite.getOrNull() ?: return emptyList()
        val siteId = LocalId(siteModel.id)
        val remoteProductId = RemoteId(productId)

        return posLocalCatalogStore.observeVariationsForProduct(siteId, remoteProductId)
            .map { result ->
                result.getOrNull()?.mapNotNull { variationModel ->
                    productStore.getVariationByRemoteId(
                        siteModel,
                        productId,
                        variationModel.remoteVariationId.value
                    )?.toWooPosVariation(mapper)
                } ?: emptyList()
            }
            .firstOrNull() ?: emptyList()
    }

    override suspend fun resetState() {
        // No-op for database mode as there's no state to reset
    }

    override fun canLoadMore(numOfVariations: Int): Boolean {
        // Database contains all variations, no pagination needed
        return false
    }

    override fun fetchFirstPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<FetchResult> = flow {
        val databaseVariations = getVariationsFromDatabase(productId).applyFilter()
        emit(FetchResult.Remote(Result.success(databaseVariations)))
    }.flowOn(Dispatchers.IO)

    override suspend fun loadMore(productId: Long): Result<List<WooPosVariation>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }
}

private fun List<WooPosVariation>.applyFilter(): List<WooPosVariation> {
    return filter { !it.isDownloadable }
}
