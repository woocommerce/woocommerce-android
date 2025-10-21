package org.wordpress.android.fluxc.store.pos.localcatalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.WooPosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPosVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToWooPOSEntity
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogStore @Inject constructor(
    private val posProductRestClient: WooPosProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val posProductDao: WooPosProductsDao,
    private val posVariationsDao: WooPosVariationsDao,
    private val headersParser: HeadersParser,
    private val database: WCAndroidDatabase,
) {
    private companion object {
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
    ): Flow<Result<List<WooPosProductEntity>>> =
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
    ): Result<WooPosProductEntity?> =
        coroutineEngine.withDefaultContext(API, this, "getProduct") {
            val product = posProductDao.getProduct(siteId, remoteProductId)
            Result.success(product)
        }

    /**
     * Gets the count of products in the local database for a given site.
     *
     * @param [siteId] The local site ID
     * @return Result containing the product count or error
     */
    suspend fun getProductCount(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Int> =
        coroutineEngine.withDefaultContext(API, this, "getProductCount") {
            val count = posProductDao.getProductCount(siteId)
            Result.success(count)
        }

    /**
     * Executes a block of code within a database transaction.
     * If the block throws an exception, the transaction is rolled back.
     *
     * @param [block] The code to execute within the transaction
     * @return Result containing the result of the block or error
     */
    suspend fun <T> executeInTransaction(
        block: suspend () -> T
    ): Result<T> =
        coroutineEngine.withDefaultContext(API, this, "executeInTransaction") {
            runCatching {
                database.executeInTransaction(block)
            }
        }

    /**
     * Fetches only the total count of products without fetching the actual product data.
     * Makes a minimal API call with per_page=1 to get the count from response headers.
     *
     * @param [site] The site to get the products count for
     * @param [modifiedAfterGmt] ISO 8601 formatted date string (GMT) to filter by modified date
     * @return Result containing the total count of products or error
     */
    suspend fun fetchProductsCount(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
    ): Result<Int> =
        coroutineEngine.withDefaultContext(API, this, "fetchProductsCount") {
            val response = posProductRestClient.fetchProducts(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                offset = 0,
                pageSize = 1
            )

            when {
                response.isError -> Result.failure(mapResponseError(response.error))

                else -> {
                    val totalCount = headersParser.getTotalCount(response)
                    if (totalCount == null) {
                        Result.failure(
                            WooPosLocalCatalogError.InvalidResponse(
                                "Missing required header in response: X-WP-Total."
                            )
                        )
                    } else {
                        Result.success(totalCount)
                    }
                }
            }
        }

    /**
     * Fetch recently modified products with pagination support.
     *
     * @param [site] The site to sync products for
     * @param [modifiedAfterGmt] ISO 8601 formatted date string (GMT)
     * @param [offset] Starting offset for pagination
     * @param [pageSize] Number of products to fetch per page (default: 100, max: 100)
     * @return Result containing SyncResponse with pagination info or error
     */
    suspend fun fetchRecentlyModifiedProducts(
        site: SiteModel,
        modifiedAfterGmt: String?,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<WooPosLocalCatalogFetchProductsResult> =
        coroutineEngine.withDefaultContext(API, this, "fetchRecentlyModifiedProducts") {
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchProducts(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                offset = offset,
                pageSize = validPageSize
            )

            val serverDate = headersParser.getServerDate(response)

            when {
                response.isError -> Result.failure(mapResponseError(response.error))

                serverDate == null -> return@withDefaultContext Result.failure(
                    WooPosLocalCatalogError.InvalidResponse("Missing required header in response: Server Date.")
                )

                response.model.isNullOrEmpty() -> Result.success(
                    WooPosLocalCatalogFetchProductsResult(
                        products = emptyList(),
                        syncedCount = 0,
                        hasMore = false,
                        nextOffset = offset,
                        totalPages = 0,
                        serverDate = serverDate
                    )
                )

                else -> {
                    val products = response.model.map { it.mapToWooPOSEntity(site.localId()) }

                    val hasMore = products.size == validPageSize

                    val totalPages = headersParser.getTotalPages(response)

                    if (totalPages == null) {
                        return@withDefaultContext Result.failure(
                            WooPosLocalCatalogError.InvalidResponse(
                                "Missing required header in response: Total Pages."
                            )
                        )
                    }

                    Result.success(
                        WooPosLocalCatalogFetchProductsResult(
                            products = products,
                            syncedCount = products.size,
                            hasMore = hasMore,
                            nextOffset = if (hasMore) offset + products.size else offset,
                            totalPages = totalPages,
                            serverDate = serverDate,
                        )
                    )
                }
            }
        }

    suspend fun upsertProducts(products: List<WooPosProductEntity>): Result<Unit> =
        runCatching { posProductDao.upsertProducts(products) }

    suspend fun deleteAllProducts(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Unit> =
        runCatching { posProductDao.deleteAllProductsForSite(siteId) }

    suspend fun upsertVariations(variations: List<WooPosVariationEntity>): Result<Unit> =
        runCatching { posVariationsDao.upsertVariations(variations) }

    suspend fun deleteAllVariations(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Unit> =
        runCatching { posVariationsDao.deleteAllVariationsForSite(siteId) }

    /**
     * Observes all variations for a given product from the local database.
     *
     * @param siteId The local site ID
     * @param productId The remote product ID
     * @return [Flow] of [Result] containing list of variations or error
     */
    fun observeVariationsForProduct(
        siteId: Int,
        productId: Long,
    ): Flow<Result<List<WooPosVariationEntity>>> =
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
        siteId: Int,
        productId: Long,
        variationId: Long
    ): Result<WooPosVariationEntity?> =
        coroutineEngine.withDefaultContext(API, this, "getVariation") {
            val variation = posVariationsDao.getVariation(siteId, productId, variationId)
            Result.success(variation)
        }

    /**
     * Fetches only the total count of variations without fetching the actual variation data.
     * Makes a minimal API call with per_page=1 to get the count from response headers.
     *
     * @param [site] The site to get the variations count for
     * @param [modifiedAfterGmt] ISO 8601 formatted date string (GMT) to filter by modified date
     * @return Result containing the total count of variations or error
     */
    suspend fun fetchVariationsCount(
        site: SiteModel,
        modifiedAfterGmt: String? = null,
    ): Result<Int> =
        coroutineEngine.withDefaultContext(API, this, "fetchVariationsCount") {
            val response = posProductRestClient.fetchVariations(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                page = 1,
                pageSize = 1
            )

            when {
                response.isError -> Result.failure(mapResponseError(response.error))

                else -> {
                    val totalCount = headersParser.getTotalCount(response)
                    if (totalCount == null) {
                        Result.failure(
                            WooPosLocalCatalogError.InvalidResponse(
                                "Missing required header in response: X-WP-Total."
                            )
                        )
                    } else {
                        Result.success(totalCount)
                    }
                }
            }
        }

    /**
     * Fetches recently modified variations with pagination support.
     *
     * @param site The site to sync variations for
     * @param modifiedAfterGmt ISO 8601 formatted date string (GMT)
     * @param page Starting page for pagination (1-based)
     * @param pageSize Number of variations to fetch per page (default: 100, max: 100)
     * @return [Result] containing [WooPosVariationsFetchResult] with pagination info or error
     */
    @Suppress("LongMethod")
    suspend fun fetchRecentlyModifiedVariations(
        site: SiteModel,
        modifiedAfterGmt: String?,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<WooPosVariationsFetchResult> =
        coroutineEngine.withDefaultContext(API, this, "fetchRecentlyModifiedVariations") {
            require(page > 0) { "Page number must be 1 or greater" }
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchVariations(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                page = page,
                pageSize = validPageSize
            )

            val serverDate = headersParser.getServerDate(response)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                serverDate == null -> return@withDefaultContext Result.failure(
                    WooPosLocalCatalogError.InvalidResponse("Missing required header in response: Server Date.")
                )

                response.model.isNullOrEmpty() -> {
                    Result.success(
                        WooPosVariationsFetchResult(
                            variations = emptyList(),
                            syncedCount = 0,
                            hasMore = false,
                            nextPage = page,
                            totalPages = 0,
                            serverDate = serverDate,
                        )
                    )
                }

                else -> {
                    val siteId = site.localId()

                    val variations = response.model.map { variationResponse ->
                        variationResponse.mapToPosVariationModel(siteId)
                    }

                    val hasMore = variations.size == validPageSize

                    val totalPages = headersParser.getTotalPages(response)

                    if (totalPages == null) {
                        return@withDefaultContext Result.failure(
                            WooPosLocalCatalogError.InvalidResponse(
                                "Missing required header in response: X-WP-TotalPages."
                            )
                        )
                    }

                    Result.success(
                        WooPosVariationsFetchResult(
                            variations = variations,
                            syncedCount = variations.size,
                            hasMore = hasMore,
                            nextPage = if (hasMore) {
                                page + 1
                            } else {
                                page
                            },
                            totalPages = totalPages,
                            serverDate = serverDate,
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
    ): Result<WooPosGenerateCatalogResult> =
        coroutineEngine.withDefaultContext(API, this, "generateCatalog") {
            val response = posProductRestClient.postGenerateCatalog(site)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                response.model == null -> {
                    Result.failure(WooPosLocalCatalogError.EmptyResponse)
                }

                response.model.jobId == null -> {
                    Result.failure(WooPosLocalCatalogError.InvalidResponse("Missing job ID in response"))
                }

                else -> {
                    val jobId = response.model.jobId.toString()
                    Result.success(
                        WooPosGenerateCatalogResult(jobId = jobId)
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
    ): Result<WooPosCatalogStatusResult> =
        coroutineEngine.withDefaultContext(API, this, "fetchCatalogStatus") {
            val response = posProductRestClient.getCatalogStatus(site, jobId)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                response.model == null -> {
                    Result.failure(WooPosLocalCatalogError.EmptyResponse)
                }

                response.model.status.isNullOrEmpty() -> {
                    Result.failure(WooPosLocalCatalogError.InvalidResponse("Missing job ID in response"))
                }

                else -> {
                    Result.success(
                        WooPosCatalogStatusResult(
                            status = response.model.status,
                            downloadUrl = response.model.downloadUrl
                        )
                    )
                }
            }
        }

    private fun mapResponseError(error: WooError?): WooPosLocalCatalogError {
        return when (error?.type) {
            WooErrorType.TIMEOUT -> WooPosLocalCatalogError.NetworkError(
                errorMessage = "Request timed out",
                code = error.type.name
            )

            WooErrorType.NO_CONNECTION -> WooPosLocalCatalogError.NetworkError(
                errorMessage = error.message ?: "No network connection",
                code = error.type.name
            )

            WooErrorType.INVALID_RESPONSE -> WooPosLocalCatalogError.NetworkError(
                errorMessage = "Invalid response from server",
                code = error.type.name
            )

            WooErrorType.API_ERROR -> WooPosLocalCatalogError.NetworkError(
                errorMessage = error.message ?: "API error occurred",
                code = error.type.name
            )

            WooErrorType.EMPTY_RESPONSE -> WooPosLocalCatalogError.EmptyResponse
            else -> WooPosLocalCatalogError.NetworkError(
                errorMessage = error?.message ?: "Unknown error occurred",
                code = error?.type?.name
            )
        }
    }
}
