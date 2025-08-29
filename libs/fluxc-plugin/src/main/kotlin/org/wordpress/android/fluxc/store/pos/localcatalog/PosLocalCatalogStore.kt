package org.wordpress.android.fluxc.store.pos.localcatalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.PosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPOSModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPosVariationModel
import org.wordpress.android.fluxc.persistence.dao.pos.PosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.PosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosVariationModel
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosLocalCatalogStore @Inject constructor(
    private val posProductRestClient: PosProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val posProductDao: PosProductsDao,
    private val posVariationsDao: PosVariationsDao,
) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * Observes all products for a given site from the local database.
     *
     * @param [siteId] The local site ID to observe products for
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
     * @param [siteId] The local site ID
     * @param [remoteProductId] The remote product ID
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
     * @param [site] The site to sync products for
     * @param [modifiedAfterGmt] ISO 8601 formatted date string (GMT)
     * @param [offset] Starting offset for pagination
     * @param [pageSize] Number of products to fetch per page (default: 100, max: 100)
     * @return Result containing SyncResponse with pagination info or error
     */
    suspend fun syncRecentlyModifiedProducts(
        site: SiteModel,
        modifiedAfterGmt: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<PosLocalCatalogSyncResult> =
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
                            PosLocalCatalogSyncResult(
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
                            PosLocalCatalogSyncResult(
                                syncedCount = products.size,
                                hasMore = hasMore,
                                nextOffset = if (hasMore) offset + products.size else offset
                            )
                        )
                    }
            }
        }

    /**
     * Observes all variations for a given product from the local database.
     *
     * @param siteId The local site ID
     * @param productId The remote product ID
     * @return [Flow] of [Result] containing list of variations or error
     */
    fun observeVariationsForProduct(
        siteId: LocalOrRemoteId.LocalId,
        productId: LocalOrRemoteId.RemoteId
    ): Flow<Result<List<WCPosVariationModel>>> =
        posVariationsDao.observeVariationsForProduct(siteId, productId)
            .map { variations ->
                Result.success(variations)
            }

    /**
     * Gets a single variation from the local database.
     *
     * @param siteId The local site ID
     * @param productId The remote product ID
     * @param variationId The remote variation ID
     * @return [Result] containing the variation if found, null if not found, or error
     */
    suspend fun getVariation(
        siteId: LocalOrRemoteId.LocalId,
        productId: LocalOrRemoteId.RemoteId,
        variationId: LocalOrRemoteId.RemoteId
    ): Result<WCPosVariationModel?> =
        coroutineEngine.withDefaultContext(API, this, "getVariation") {
            val variation = posVariationsDao.getVariation(siteId, productId, variationId)
            Result.success(variation)
        }

    /**
     * Syncs recently modified variations with pagination support.
     *
     * @param site The site to sync variations for
     * @param modifiedAfterGmt ISO 8601 formatted date string (GMT)
     * @param page Starting page for pagination (1-based)
     * @param pageSize Number of variations to fetch per page (default: 100, max: 100)
     * @return [Result] containing [PosVariationsSyncResult] with pagination info or error
     */
    suspend fun syncRecentlyModifiedVariations(
        site: SiteModel,
        modifiedAfterGmt: String,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<PosVariationsSyncResult> =
        coroutineEngine.withDefaultContext(API, this, "syncRecentlyModifiedVariations") {
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchVariations(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                page = page,
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
                        PosVariationsSyncResult(
                            syncedCount = 0,
                            hasMore = false,
                            nextPage = page
                        )
                    )
                }

                else -> {
                    val siteId = site.localId()

                    val variations = response.model.map { variationResponse ->
                        variationResponse.mapToPosVariationModel(siteId)
                    }

                    val upsertResult = runCatching {
                        posVariationsDao.upsertVariations(variations)
                    }

                    if (upsertResult.isFailure) {
                        return@withDefaultContext Result.failure(
                            PosLocalCatalogError.DatabaseError(
                                errorMessage = "Failed to save variations to database",
                                throwable = upsertResult.exceptionOrNull()
                            )
                        )
                    }

                    val hasMore = variations.size == validPageSize

                    Result.success(
                        PosVariationsSyncResult(
                            syncedCount = variations.size,
                            hasMore = hasMore,
                            nextPage = if (hasMore) {
                                page + 1
                            } else {
                                page
                            }
                        )
                    )
                }
            }
        }

    /**
     * Generates a new catalog on the server.
     *
     * @param [site] The site to generate catalog for
     * @return [Result] containing PosGenerateCatalogResult with job ID or error
     */
    suspend fun generateCatalog(
        site: SiteModel
    ): Result<PosGenerateCatalogResult> =
        coroutineEngine.withDefaultContext(API, this, "generateCatalog") {
            val response = posProductRestClient.postGenerateCatalog(site)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                response.model == null -> {
                    Result.failure(PosLocalCatalogError.EmptyResponse)
                }

                response.model.jobId == null -> {
                    Result.failure(PosLocalCatalogError.InvalidResponse("Missing job ID in response"))
                }

                else -> {
                    val jobId = response.model.jobId.toString()
                    Result.success(
                        PosGenerateCatalogResult(jobId = jobId)
                    )
                }
            }
        }

    /**
     * Fetches the status of a catalog generation job.
     *
     * @param [site] The site to check catalog status for
     * @param [jobId] The job ID from generateCatalog
     * @return [Result] containing PosCatalogStatusResult with status and download URL or error
     */
    suspend fun fetchCatalogStatus(
        site: SiteModel,
        jobId: String
    ): Result<PosCatalogStatusResult> =
        coroutineEngine.withDefaultContext(API, this, "fetchCatalogStatus") {
            val response = posProductRestClient.getCatalogStatus(site, jobId)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                response.model == null -> {
                    Result.failure(PosLocalCatalogError.EmptyResponse)
                }

                response.model.status.isNullOrEmpty() -> {
                    Result.failure(PosLocalCatalogError.InvalidResponse("Missing job ID in response"))
                }

                else -> {
                    Result.success(
                        PosCatalogStatusResult(
                            status = response.model.status,
                            downloadUrl = response.model.downloadUrl
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
