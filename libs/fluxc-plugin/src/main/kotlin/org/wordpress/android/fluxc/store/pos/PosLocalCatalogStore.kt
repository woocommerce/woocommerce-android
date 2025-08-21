package org.wordpress.android.fluxc.store.pos

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.PosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPOSModel
import org.wordpress.android.fluxc.persistence.dao.pos.PosProductsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

sealed class ProductSyncResult {
    data class Success(
        val syncedCount: Int,
        val hasMore: Boolean,
        val nextOffset: Int
    ) : ProductSyncResult()

    data class Failure(
        val error: PosLocalCatalogError
    ) : ProductSyncResult()
}

sealed class PosLocalCatalogError {
    data class NetworkError(val message: String, val code: String? = null) : PosLocalCatalogError()
    data class DatabaseError(val message: String, val throwable: Throwable? = null) : PosLocalCatalogError()
    object EmptyResponse : PosLocalCatalogError()
    data class UnknownError(val throwable: Throwable) : PosLocalCatalogError()
}

@Singleton
class PosLocalCatalogStore @Inject constructor(
    private val posProductRestClient: PosProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val posProductDao: PosProductsDao,
) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 100
    }

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
        modifiedAfterGmt: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): ProductSyncResult =
        coroutineEngine.withDefaultContext(API, this, "syncRecentlyModifiedProducts") {
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            try {
                val response = posProductRestClient.fetchProducts(
                    site = site,
                    modifiedAfter = modifiedAfterGmt,
                    offset = offset,
                    pageSize = validPageSize
                )

                when {
                    response.isError -> {
                        ProductSyncResult.Failure(
                            error = mapResponseError(response.error),
                        )
                    }

                    response.model.isNullOrEmpty() -> {
                        // Empty response is valid - means no more products
                        ProductSyncResult.Success(
                            syncedCount = 0,
                            hasMore = false,
                            nextOffset = offset
                        )
                    }

                    else -> {
                        val products = response.model.map { it.mapToPOSModel() }

                        val upsertResult = runCatching {
                            posProductDao.upsertProducts(products)
                        }

                        if (upsertResult.isFailure) {
                            return@withDefaultContext ProductSyncResult.Failure(
                                error = PosLocalCatalogError.DatabaseError(
                                    message = "Failed to save products to database",
                                    throwable = upsertResult.exceptionOrNull()
                                ),
                            )
                        }

                        val hasMore = products.size == validPageSize

                        ProductSyncResult.Success(
                            syncedCount = products.size,
                            hasMore = hasMore,
                            nextOffset = if (hasMore) offset + products.size else offset
                        )
                    }
                }
            } catch (e: Exception) {
                ProductSyncResult.Failure(
                    error = PosLocalCatalogError.UnknownError(e)
                )
            }
        }

    private fun mapResponseError(error: WooError?): PosLocalCatalogError {
        return when (error?.type) {
            WooErrorType.TIMEOUT -> PosLocalCatalogError.NetworkError("Request timed out", error.type.name)
            WooErrorType.NO_CONNECTION -> PosLocalCatalogError.NetworkError(
                error.message ?: "No network connection",
                error.type.name
            )

            WooErrorType.INVALID_RESPONSE -> PosLocalCatalogError.NetworkError(
                "Invalid response from server",
                error.type.name
            )

            WooErrorType.API_ERROR -> PosLocalCatalogError.NetworkError(
                error.message ?: "API error occurred",
                error.type.name
            )

            WooErrorType.EMPTY_RESPONSE -> PosLocalCatalogError.EmptyResponse
            else -> PosLocalCatalogError.NetworkError(
                error?.message ?: "Unknown error occurred",
                error?.type?.name
            )
        }
    }
}
