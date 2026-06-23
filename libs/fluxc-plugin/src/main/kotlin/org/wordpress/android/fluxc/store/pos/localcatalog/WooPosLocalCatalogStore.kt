package org.wordpress.android.fluxc.store.pos.localcatalog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.WooPosProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToPosVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.mapToWooPOSEntity
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
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
    private val posFtsDao: WooPosSearchableFtsDao,
    private val headersParser: HeadersParser,
    private val database: WCAndroidDatabase,
) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 100

        private val UNICODE61_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

        // Split on the same boundaries as the unicode61 tokenizer (non-letter, non-digit)
        // so that queries like "t-shirt" or "SKU-1234" match the indexed tokens.
        fun formatFtsQuery(rawQuery: String): String {
            return rawQuery
                .trim()
                .split(UNICODE61_SEPARATORS)
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
        }
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
     * Finds a product by its unique identifier (globalUniqueId) from the local database. It returns even products
     * that are not supported in the POS, so the business logic can display Unsupported Product message instead of
     * displaying Not Found message.
     *
     * @param [siteId] The local site ID
     * @param [identifier] The unique identifier to search for
     * @return Result containing the product if found, null if not found, or error
     */
    suspend fun findEvenUnsupportedProductByIdentifier(
        siteId: LocalOrRemoteId.LocalId,
        identifier: String
    ): Result<WooPosProductEntity?> =
        coroutineEngine.withDefaultContext(API, this, "findEvenUnsupportedProductByIdentifier") {
            val product = posProductDao.findProductByIdentifier(siteId, identifier)
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
     * Gets a paginated list of products from the local database.
     *
     * @param [siteId] The local site ID
     * @param [pageSize] The number of results to return
     * @param [offset] The offset for pagination
     * @return Result containing the list of products or error
     */
    suspend fun getProducts(
        siteId: LocalOrRemoteId.LocalId,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<WooPosProductEntity>> =
        coroutineEngine.withDefaultContext(API, this, "getProducts") {
            val products = posProductDao.getProducts(
                localSiteId = siteId,
                limit = pageSize.coerceIn(1, MAX_PAGE_SIZE),
                offset = offset.coerceAtLeast(0)
            )
            Result.success(products)
        }

    /**
     * Searches products in the local database by name, SKU, or global unique ID.
     *
     * @param [siteId] The local site ID
     * @param [searchQuery] The search query string
     * @param [pageSize] The number of results to return
     * @param [offset] The offset for pagination
     * @return Result containing the list of matching products or error
     */
    suspend fun searchProducts(
        siteId: LocalOrRemoteId.LocalId,
        searchQuery: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<WooPosProductEntity>> =
        coroutineEngine.withDefaultContext(API, this, "searchProducts") {
            val products = posProductDao.searchProducts(
                localSiteId = siteId,
                searchQuery = searchQuery,
                limit = pageSize.coerceAtMost(MAX_PAGE_SIZE),
                offset = offset
            )
            Result.success(products)
        }

    suspend fun searchProductsFts(
        siteId: LocalOrRemoteId.LocalId,
        searchQuery: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<WooPosFtsSearchResult>> =
        coroutineEngine.withDefaultContext(API, this, "searchProductsFts") {
            val ftsQuery = formatFtsQuery(searchQuery)
            if (ftsQuery.isBlank()) {
                return@withDefaultContext Result.success(emptyList())
            }

            val ftsResults = posFtsDao.search(
                localSiteId = siteId.value.toString(),
                query = ftsQuery,
                limit = pageSize.coerceAtMost(MAX_PAGE_SIZE),
                offset = offset
            )

            if (ftsResults.isEmpty()) {
                return@withDefaultContext Result.success(emptyList())
            }

            val productIds = ftsResults
                .filter { it.parentProductId.isBlank() }
                .map { LocalOrRemoteId.RemoteId(it.itemId.toLong()) }
            val variationIds = ftsResults
                .filter { it.parentProductId.isNotBlank() }
                .map { LocalOrRemoteId.RemoteId(it.itemId.toLong()) }

            // Batch-fetch full entities in 2 queries (products + variations) instead of
            // fetching one-by-one per FTS result, to avoid N separate DB hits.
            val productsMap = if (productIds.isNotEmpty()) {
                posProductDao.getProductsByIds(siteId, productIds)
                    .associateBy { it.remoteId.value }
            } else {
                emptyMap()
            }

            val variationsMap = if (variationIds.isNotEmpty()) {
                posVariationsDao.getVariationsByIds(siteId, variationIds)
                    .associateBy { it.remoteVariationId.value }
            } else {
                emptyMap()
            }

            val results = ftsResults.mapNotNull { ftsEntity ->
                val itemId = ftsEntity.itemId.toLong()
                if (ftsEntity.parentProductId.isBlank()) {
                    productsMap[itemId]?.let { WooPosFtsSearchResult.Product(it) }
                } else {
                    variationsMap[itemId]?.let {
                        WooPosFtsSearchResult.Variation(
                            entity = it,
                            parentProductName = ftsEntity.name,
                        )
                    }
                }
            }

            Result.success(results)
        }

    /**
     * Gets the count of variations in the local database for a given site.
     *
     * @param [siteId] The local site ID
     * @return Result containing the variation count or error
     */
    suspend fun getVariationCount(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Int> =
        coroutineEngine.withDefaultContext(API, this, "getVariationCount") {
            val count = posVariationsDao.getVariationCount(siteId)
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
            runCatchingCancellable {
                database.executeInTransaction(block)
            }
        }

    /**
     * Fetch recently modified products with pagination support.
     *
     * @param [site] The site to sync products for
     * @param [modifiedAfterGmt] ISO 8601 formatted date string (GMT)
     * @param [page] Page for pagination.
     * @param [pageSize] Number of products to fetch per page (default: 100, max: 100)
     * @return Result containing SyncResponse with pagination info or error
     */
    suspend fun fetchRecentlyModifiedProducts(
        site: SiteModel,
        posProductsOnly: Boolean,
        modifiedAfterGmt: String? = null,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        includeStatus: List<CoreProductStatus>? = null,
    ): Result<WooPosPaginatedFetchResult<WooPosProductEntity>> =
        coroutineEngine.withDefaultContext(API, this, "fetchRecentlyModifiedProducts") {
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchProducts(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                page = page,
                pageSize = validPageSize,
                posProductsOnly = posProductsOnly,
                includeStatus = includeStatus,
            )

            val serverDate = headersParser.getServerDate(response)

            when {
                response.isError -> Result.failure(mapResponseError(response.error))

                serverDate == null -> return@withDefaultContext Result.failure(
                    WooPosLocalCatalogError.InvalidResponse("Missing required header in response: Server Date.")
                )

                response.model.isNullOrEmpty() -> Result.success(
                    WooPosPaginatedFetchResult(
                        items = emptyList(),
                        syncedCount = 0,
                        hasMore = false,
                        nextPage = page,
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
                        WooPosPaginatedFetchResult(
                            items = products,
                            syncedCount = products.size,
                            hasMore = hasMore,
                            nextPage = if (hasMore) page + 1 else page,
                            totalPages = totalPages,
                            serverDate = serverDate,
                        )
                    )
                }
            }
        }

    suspend fun upsertProducts(products: List<WooPosProductEntity>): Result<Unit> =
        runCatchingCancellable { posProductDao.upsertProducts(products) }

    suspend fun storeCatalogData(
        localSiteId: LocalOrRemoteId.LocalId,
        products: List<WooPosProductEntity>,
        variations: List<WooPosVariationEntity>
    ): Result<Unit> =
        coroutineEngine.withDefaultContext(API, this, "storeCatalogData") {
            database.executeInTransaction {
                posProductDao.deleteAllProductsForSite(localSiteId)
                posVariationsDao.deleteAllVariationsForSite(localSiteId)

                if (products.isNotEmpty()) {
                    posProductDao.upsertProducts(products)
                }

                if (variations.isNotEmpty()) {
                    posVariationsDao.upsertVariations(variations)
                }
            }
            Result.success(Unit)
        }

    suspend fun deleteAllProducts(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Unit> =
        runCatchingCancellable { posProductDao.deleteAllProductsForSite(siteId) }

    suspend fun deleteProducts(
        siteId: LocalOrRemoteId.LocalId,
        productIds: List<LocalOrRemoteId.RemoteId>
    ): Result<Unit> =
        runCatchingCancellable {
            database.executeInTransaction {
                posProductDao.deleteProducts(siteId, productIds)
                posVariationsDao.deleteVariationsForProducts(siteId, productIds)
            }
        }

    suspend fun deleteVariations(
        siteId: LocalOrRemoteId.LocalId,
        variations: List<Pair<LocalOrRemoteId.RemoteId, LocalOrRemoteId.RemoteId>>
    ): Result<Unit> =
        runCatchingCancellable {
            database.executeInTransaction {
                val variationIds = variations.map { it.second }
                posVariationsDao.deleteVariationsByIds(siteId, variationIds)
            }
        }

    suspend fun upsertVariations(variations: List<WooPosVariationEntity>): Result<Unit> =
        runCatchingCancellable { posVariationsDao.upsertVariations(variations) }

    suspend fun deleteAllVariations(
        siteId: LocalOrRemoteId.LocalId
    ): Result<Unit> =
        runCatchingCancellable { posVariationsDao.deleteAllVariationsForSite(siteId) }

    /**
     * Observes all variations for a given product from the local database.
     *
     * @param localSiteId The local site ID
     * @param productId The remote product ID
     * @return [Flow] of [Result] containing list of variations or error
     */
    fun observeVariationsForProduct(
        localSiteId: LocalOrRemoteId.LocalId,
        productId: Long,
    ): Flow<Result<List<WooPosVariationEntity>>> =
        posVariationsDao.observeVariationsForProduct(localSiteId.value, productId)
            .map { variations ->
                Result.success(variations)
            }

    /**
     * Gets a single variation from the local database.
     *
     * @param localSiteId The local site ID
     * @param productId The remote product ID
     * @param variationId The remote variation ID
     * @return [Result] containing the variation if found, null if not found, or error
     */
    suspend fun getVariation(
        localSiteId: LocalOrRemoteId.LocalId,
        productId: Long,
        variationId: Long
    ): Result<WooPosVariationEntity?> =
        coroutineEngine.withDefaultContext(API, this, "getVariation") {
            val variation = posVariationsDao.getVariation(localSiteId.value, productId, variationId)
            Result.success(variation)
        }

    /**
     * Finds a variation by its unique identifier (globalUniqueId) from the local database. It returns even variations
     * that are not supported in the POS, so the business logic can display Unsupported Variation message instead of
     * displaying Not Found message.
     *
     * @param [siteId] The local site ID
     * @param [identifier] The unique identifier to search for
     * @return Result containing the variation if found, null if not found, or error
     */
    suspend fun findEvenUnsupportedVariationByIdentifier(
        siteId: LocalOrRemoteId.LocalId,
        identifier: String
    ): Result<WooPosVariationEntity?> =
        coroutineEngine.withDefaultContext(API, this, "findEvenUnsupportedVariationByIdentifier") {
            val variation = posVariationsDao.findVariationByIdentifier(siteId, identifier)
            Result.success(variation)
        }

    /**
     * Fetches recently modified variations with pagination support.
     *
     * @param site The site to sync variations for
     * @param modifiedAfterGmt ISO 8601 formatted date string (GMT)
     * @param page Starting page for pagination (1-based)
     * @param pageSize Number of variations to fetch per page (default: 100, max: 100)
     * @return [Result] containing [WooPosPaginatedFetchResult] with pagination info or error
     */
    @Suppress("LongMethod")
    suspend fun fetchRecentlyModifiedVariations(
        site: SiteModel,
        posProductsOnly: Boolean,
        modifiedAfterGmt: String? = null,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Result<WooPosPaginatedFetchResult<WooPosVariationEntity>> =
        coroutineEngine.withDefaultContext(API, this, "fetchRecentlyModifiedVariations") {
            require(page > 0) { "Page number must be 1 or greater" }
            val validPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)

            val response = posProductRestClient.fetchVariations(
                site = site,
                modifiedAfter = modifiedAfterGmt,
                page = page,
                pageSize = validPageSize,
                posProductsOnly = posProductsOnly,
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
                        WooPosPaginatedFetchResult(
                            items = emptyList(),
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
                        WooPosPaginatedFetchResult(
                            items = variations,
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
     * @param [force] When true, asks the server to discard any prior attempt and start a clean
     * generation. Should only be set on the first request of a manually-invoked sync.
     * @return [Result] containing PosGenerateCatalogResult with catalog generation details or error
     */
    suspend fun generateCatalogOrGetStatus(
        site: SiteModel,
        force: Boolean = false
    ): Result<WooPosGenerateCatalogResult> =
        coroutineEngine.withDefaultContext(API, this, "generateCatalog") {
            val response = posProductRestClient.postGenerateCatalog(site, force)

            when {
                response.isError -> {
                    Result.failure(
                        mapResponseError(response.error)
                    )
                }

                response.model == null -> {
                    Result.failure(WooPosLocalCatalogError.EmptyResponse)
                }

                else -> {
                    Result.success(
                        WooPosGenerateCatalogResult(
                            scheduledAt = response.model.scheduledAt,
                            completedAt = response.model.completedAt,
                            state = WooPosGenerateCatalogState.from(response.model.state),
                            progress = response.model.progress,
                            processed = response.model.processed,
                            total = response.model.total,
                            url = response.model.url,
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

    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        runCatching(block).also { result ->
            val error = result.exceptionOrNull()
            if (error is CancellationException) throw error
        }
}
