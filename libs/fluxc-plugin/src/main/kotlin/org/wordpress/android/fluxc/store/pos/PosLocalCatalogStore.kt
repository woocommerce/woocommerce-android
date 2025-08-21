package org.wordpress.android.fluxc.store.pos

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.PosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPOSModel
import org.wordpress.android.fluxc.persistence.dao.pos.PosProductsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosLocalCatalogStore @Inject constructor(
    private val posProductRestClient: PosProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val posProductDao: PosProductsDao,
) {
    suspend fun observeProducts(siteId: LocalOrRemoteId.LocalId) =
        coroutineEngine.withDefaultContext(API, this, "observeProducts") {
            posProductDao.observeAllProducts(siteId)
        }

    suspend fun getProduct(
        siteId: LocalOrRemoteId.LocalId,
        remoteProductId: LocalOrRemoteId.RemoteId
    ) = coroutineEngine.withDefaultContext(API, this, "getProduct") {
        posProductDao.getProduct(siteId, remoteProductId)
    }

    suspend fun syncRecentlyModifiedProducts(
        site: SiteModel,
        modifiedAfter: String,
        offset: Int,
        pageSize: Int = 100,
    ): Result<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchRecentlyModifiedProducts") {
            val response = posProductRestClient.fetchProducts(site, modifiedAfter, offset, pageSize)
            if (!response.isError && !response.model.isNullOrEmpty()) {
                posProductDao.upsertProducts(response.model.map { it.mapToPOSModel() })
                Result.success(Unit)
            } else {
                // TODO: Handle error properly
                Result.failure(IllegalStateException("Failed to fetch products: ${response.error}"))
            }
        }
}
