package org.wordpress.android.fluxc.store.pos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.PosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPOSModel
import org.wordpress.android.fluxc.persistence.dao.pos.PosProductsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val syncedCount: Int,
    val hasMore: Boolean,
    val nextOffset: Int
)

sealed class PosLocalCatalogError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class NetworkError(
        val errorMessage: String,
        val code: String? = null
    ) : PosLocalCatalogError(errorMessage)

    data class DatabaseError(
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : PosLocalCatalogError(errorMessage, throwable)

    object EmptyResponse : PosLocalCatalogError("Empty response from server")

    data class UnknownError(
        val throwable: Throwable
    ) : PosLocalCatalogError(throwable.message ?: "Unknown error", throwable)
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

    /**
     * Observes all products for a given site from the local database.
     *
     * @param siteId The local site ID to observe products for
     * @return Flow of Result containing list of products or error
     */
    fun observeProducts(
        siteId: LocalOrRemoteId.LocalId
    ): Flow<Result<List<WCPosProductModel>>> =
        posProductDao.observeAllProducts(siteId)
            .map { products ->
                Result.success(products)
            }

    /**
     * Gets a single product from the local database.
     *
     * @param siteId The local site ID
     * @param remoteProductId The remote product ID
     * @return Result containing the product if found, null if not found, or error
     */
    suspend fun getProduct(
        siteId: LocalOrRemoteId.LocalId,
        remoteProductId: LocalOrRemoteId.RemoteId
    ): Result<WCPosProductModel?> =
        coroutineEngine.withDefaultContext(API, this, "getProduct") {
            val product = posProductDao.getProduct(siteId, remoteProductId)
            Result.success(product)
        }

    /**
     * Syncs recently modified products with pagination support.
     *
     * @param site The site to sync products for
     * @param modifiedAfterGmt ISO 8601 formatted date string (GMT)
     * @param offset Starting offset for pagination
     * @param pageSize Number of products to fetch per page (default: 100, max: 100)
     * @return Result containing SyncResponse with pagination info or error
     */
    suspend fun syncRecentlyModifiedProducts(
        site: SiteModel,
        modifiedAfterGmt: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<SyncResult> =
        coroutineEngine.withDefaultContext(API, this, "syncRecentlyModifiedProducts") {
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchProducts(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                offset = offset,
                pageSize = validPageSize
            )

            when {
                    response.isError -> {
                        Result.failure(
                            mapResponseError(response.error)
                        )
                    }

                    response.model.isNullOrEmpty() -> {
                        Result.success(
                            SyncResult(
                                syncedCount = 0,
                                hasMore = false,
                                nextOffset = offset
                            )
                        )
                    }

                    else -> {
                        val products = response.model.map { it.mapToPOSModel() }

                        val upsertResult = runCatching {
                            posProductDao.upsertProducts(products)
                        }

                        if (upsertResult.isFailure) {
                            return@withDefaultContext Result.failure(
                                PosLocalCatalogError.DatabaseError(
                                    errorMessage = "Failed to save products to database",
                                    throwable = upsertResult.exceptionOrNull()
                                )
                            )
                        }

                        val hasMore = products.size == validPageSize

                        Result.success(
                            SyncResult(
                                syncedCount = products.size,
                                hasMore = hasMore,
                                nextOffset = if (hasMore) offset + products.size else offset
                            )
                        )
                    }
            }
        }

    private fun mapResponseError(error: WooError?): PosLocalCatalogError {
        return when (error?.type) {
            WooErrorType.TIMEOUT -> PosLocalCatalogError.NetworkError(
                errorMessage = "Request timed out",
                code = error.type.name
            )
            WooErrorType.NO_CONNECTION -> PosLocalCatalogError.NetworkError(
                errorMessage = error.message ?: "No network connection",
                code = error.type.name
            )
            WooErrorType.INVALID_RESPONSE -> PosLocalCatalogError.NetworkError(
                errorMessage = "Invalid response from server",
                code = error.type.name
            )
            WooErrorType.API_ERROR -> PosLocalCatalogError.NetworkError(
                errorMessage = error.message ?: "API error occurred",
                code = error.type.name
            )
            WooErrorType.EMPTY_RESPONSE -> PosLocalCatalogError.EmptyResponse
            else -> PosLocalCatalogError.NetworkError(
                errorMessage = error?.message ?: "Unknown error occurred",
                code = error?.type?.name
            )
        }
    }
}
