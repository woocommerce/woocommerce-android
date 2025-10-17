package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosProductsInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val mapper: WooPosProductModelMapper
) : WooPosProductsDataSourceInterface {

    private fun getProductsFromDatabaseFlow(): Flow<List<WooPosProductModel>> {
        val siteModel = selectedSite.getOrNull() ?: return flow { emit(emptyList()) }
        val siteId = LocalOrRemoteId.LocalId(siteModel.id)

        return posLocalCatalogStore.observeProducts(siteId)
            .map { result ->
                result.getOrNull()?.map { entity ->
                    mapper.fromEntity(entity)
                } ?: emptyList()
            }
    }

    override fun fetchFirstPage(
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.ProductsResult> = getProductsFromDatabaseFlow()
        .map { products ->
            WooPosProductsDataSource.ProductsResult.Remote(Result.success(products))
        }
        .flowOn(Dispatchers.IO)

    override suspend fun loadMore(): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override val hasMorePages: Boolean = false

    override suspend fun resetState() = Unit
}
