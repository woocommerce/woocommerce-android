package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import com.woocommerce.android.ui.woopos.home.items.search.WooPosProductsSearchInDbDataSource
import com.woocommerce.android.ui.woopos.home.items.search.WooPosSearchProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogVariationSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosCatalogFileBlockedException
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFileBasedSyncAction
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsWooBelowCatalogFixVersion
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncWithFts
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformInstantCatalogFullSync
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncVariationResult
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val remoteDataSource: WooPosProductsRemoteDataSource,
    private val localDbDataSource: WooPosProductsInDbDataSource,
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
    private val syncRepository: WooPosLocalCatalogSyncRepository,
    private val isWooBelowCatalogFixVersion: WooPosIsWooBelowCatalogFixVersion,
) {
    enum class SyncStrategy {
        REMOTE,
        LOCAL_CATALOG_FILE,
    }

    private var activeSource: WooPosProductsDataSourceInterface? = null

    private var lastSyncFellBackDueToCatalogBlock = false

    fun getCurrentSyncStrategy(): SyncStrategy {
        return when (activeSource) {
            localDbDataSource -> SyncStrategy.LOCAL_CATALOG_FILE
            remoteDataSource -> SyncStrategy.REMOTE
            else -> error("Unknown sync strategy")
        }
    }

    fun didFallBackDueToCatalogBlock(): Boolean = lastSyncFellBackDueToCatalogBlock

    fun prepopulateCache(): Flow<WooPosPrepopulatingDataStatus> = channelFlow {
        lastSyncFellBackDueToCatalogBlock = false
        when (val requirement = syncStatusChecker.checkSyncRequirement()) {
            is WooPosFullSyncRequirement.LocalCatalogDisabled -> {
                activeSource = remoteDataSource
                remoteDataSource.prepopulateCache().fold(
                    onSuccess = {
                        send(WooPosPrepopulatingDataStatus.Completed)
                    },
                    onFailure = {
                        send(WooPosPrepopulatingDataStatus.Failed(it.message ?: "Unknown error"))
                    }
                )
            }

            is WooPosFullSyncRequirement.NotRequired,
            is WooPosFullSyncRequirement.NonBlockingRequired -> {
                activeSource = localDbDataSource
                send(WooPosPrepopulatingDataStatus.Completed)
            }

            is WooPosFullSyncRequirement.BlockingRequired -> blockingFullSync()

            is WooPosFullSyncRequirement.Error -> {
                send(WooPosPrepopulatingDataStatus.Failed(requirement.message))
            }
        }
    }

    private suspend fun ProducerScope<WooPosPrepopulatingDataStatus>.blockingFullSync() {
        send(WooPosPrepopulatingDataStatus.Syncing)
        activeSource = localDbDataSource

        val progressJob = launch {
            syncRepository.syncState.collect { state ->
                when (state) {
                    is WooPosFileBasedSyncAction.SyncState.Preparing ->
                        send(WooPosPrepopulatingDataStatus.SyncPreparing)

                    is WooPosFileBasedSyncAction.SyncState.Progress ->
                        send(
                            WooPosPrepopulatingDataStatus.SyncProgress(
                                processed = state.processed,
                                total = state.total
                            )
                        )

                    null -> Unit
                }
            }
        }

        localDbDataSource.prepopulateCache().fold(
            onSuccess = {
                progressJob.cancel()
                send(WooPosPrepopulatingDataStatus.Completed)
            },
            onFailure = { error ->
                progressJob.cancel()
                if (error is WooPosCatalogFileBlockedException && isWooBelowCatalogFixVersion()) {
                    prepopulateFromRemoteAsFallback()
                } else {
                    send(
                        WooPosPrepopulatingDataStatus.Failed(
                            error = error.message ?: "Unknown error",
                            isServerPermissionsError = error is WooPosCatalogFileBlockedException
                        )
                    )
                }
            }
        )
    }

    /**
     * Switches the active source to the legacy remote loader and prepopulates from it. Used both
     * for the silent fallback (Woo < 11) and the user-triggered "Continue with basic sync" skip
     * shown on the catalog-blocked error screen (Woo >= 11).
     */
    fun fallBackToRemoteDueToCatalogBlock(): Flow<WooPosPrepopulatingDataStatus> = channelFlow {
        prepopulateFromRemoteAsFallback()
    }

    private suspend fun ProducerScope<WooPosPrepopulatingDataStatus>.prepopulateFromRemoteAsFallback() {
        lastSyncFellBackDueToCatalogBlock = true
        activeSource = remoteDataSource
        remoteDataSource.prepopulateCache().fold(
            onSuccess = { send(WooPosPrepopulatingDataStatus.Completed) },
            onFailure = { send(WooPosPrepopulatingDataStatus.Failed(it.message ?: "Unknown error")) }
        )
    }

    fun fetchFirstPage(forceRefresh: Boolean): Flow<ProductsResult> =
        activeSource?.fetchFirstProductsPage(forceRefresh)
            ?: error("FetchFirstPage - Data source not selected")

    suspend fun loadMore(): Result<List<WooPosProductModel>> {
        return activeSource?.loadMoreProducts() ?: error("LoadMore - Data source not selected")
    }

    val hasMorePages: Boolean
        get() = activeSource?.hasMoreProductsPages ?: error("hasMorePages - Data source not selected")

    suspend fun getProductById(productId: LocalOrRemoteId.RemoteId): WooPosProductModel? {
        checkNotNull(activeSource) { "GetProductById - Data source not selected" }
        return activeSource?.getProductById(productId)
    }

    suspend fun resetVariationsListHandler() {
        remoteDataSource.resetVariationsListHandler()
        localDbDataSource.resetVariationsListHandler()
    }

    fun fetchVariationsFirstPage(
        productId: Long,
        forceRefresh: Boolean = true
    ): Flow<VariationsResult> =
        activeSource?.fetchFirstVariationsPage(productId, forceRefresh)
            ?: error("FetchVariationsFirstPage - Data source not selected")

    suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>> {
        return activeSource?.loadMoreVariations(productId)
            ?: error("LoadMoreVariations - Data source not selected")
    }

    fun canLoadMoreVariations(numOfVariations: Int): Boolean =
        activeSource?.canLoadMoreVariations(numOfVariations)
            ?: error("canLoadMoreVariations - Data source not selected")

    suspend fun refreshProducts(): Result<WooPosSyncProductResult> =
        activeSource?.refreshProducts()
            ?: error("refreshProducts - Data source not selected")

    suspend fun refreshVariations(productId: Long): Result<WooPosSyncVariationResult> =
        activeSource?.refreshVariations(productId)
            ?: error("refreshVariations - Data source not selected")

    suspend fun getVariationById(
        productId: Long,
        variationId: Long
    ): WooPosVariation? {
        checkNotNull(activeSource) { "GetVariationById - Data source not selected" }
        return activeSource?.getVariationById(productId, variationId)
    }

    fun searchProducts(query: String): Flow<SearchProductsResult> =
        activeSource?.searchProducts(query)
            ?: error("searchProducts - Data source not selected")

    suspend fun loadMoreSearchResults(query: String): Result<List<WooPosProductModel>> =
        activeSource?.loadMoreSearchResults(query)
            ?: error("loadMoreSearchResults - Data source not selected")

    val hasMoreSearchPages: Boolean
        get() = activeSource?.hasMoreSearchPages ?: error("hasMoreSearchPages - Data source not selected")

    sealed class WooPosPrepopulatingDataStatus {
        data object Syncing : WooPosPrepopulatingDataStatus()
        data object SyncPreparing : WooPosPrepopulatingDataStatus()
        data class SyncProgress(val processed: Int, val total: Int) : WooPosPrepopulatingDataStatus()
        data object Completed : WooPosPrepopulatingDataStatus()
        data class Failed(
            val error: String,
            val isServerPermissionsError: Boolean = false
        ) : WooPosPrepopulatingDataStatus()
    }
}

class WooPosProductsInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val productMapper: WooPosProductModelMapper,
    private val variationMapper: WooPosVariationMapper,
    private val performInstantCatalogFullSync: WooPosPerformInstantCatalogFullSync,
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val localCatalogSearchDataSource: WooPosProductsSearchInDbDataSource,
    private val syncWithFts: WooPosLocalCatalogSyncWithFts,
) : WooPosProductsDataSourceInterface {

    private val offset = AtomicInteger(0)
    private val canLoadMore = AtomicBoolean(false)
    private val accumulatedProducts = mutableListOf<WooPosProductModel>()

    override fun fetchFirstProductsPage(
        forceRefresh: Boolean
    ): Flow<ProductsResult> = flow {
        offset.set(0)
        canLoadMore.set(false)
        accumulatedProducts.clear()

        val products = loadNextProductPage()
        accumulatedProducts.addAll(products)

        emit(ProductsResult.Remote(Result.success(accumulatedProducts.toList())))
    }.flowOn(Dispatchers.IO)

    override suspend fun loadMoreProducts(): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        if (!canLoadMore.get()) {
            return@withContext Result.success(accumulatedProducts.toList())
        }

        val products = loadNextProductPage()
        accumulatedProducts.addAll(products)

        Result.success(accumulatedProducts.toList())
    }

    override val hasMoreProductsPages: Boolean
        get() = canLoadMore.get()

    private suspend fun loadNextProductPage(): List<WooPosProductModel> {
        val siteModel = selectedSite.getOrNull() ?: return emptyList()
        val siteId = LocalOrRemoteId.LocalId(siteModel.id)

        val result = posLocalCatalogStore.getProducts(
            siteId = siteId,
            pageSize = PAGE_SIZE,
            offset = offset.get()
        )

        val products: List<WooPosProductModel> = result.getOrNull()?.map { entity ->
            productMapper.fromEntity(entity)
        } ?: emptyList()

        canLoadMore.set(products.size == PAGE_SIZE)
        offset.addAndGet(PAGE_SIZE)

        return products
    }

    override suspend fun getProductById(productId: LocalOrRemoteId.RemoteId): WooPosProductModel? {
        val result = posLocalCatalogStore.getProduct(
            siteId = LocalOrRemoteId.LocalId(selectedSite.get().id),
            remoteProductId = productId
        )
        return result.getOrNull()?.let { entity ->
            productMapper.fromEntity(entity)
        }
    }

    override suspend fun resetVariationsListHandler() = Unit

    override suspend fun prepopulateCache(): Result<Unit> {
        val result = performInstantCatalogFullSync()
        if (result.isSuccess) {
            selectedSite.getOrNull()?.let { site ->
                syncWithFts.ensureFtsPopulated(site)
            }
        }
        return result
    }

    override fun fetchFirstVariationsPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<VariationsResult> {
        val siteModel = selectedSite.getOrNull() ?: return flow {
            emit(VariationsResult.Remote(Result.failure(Exception("Site not selected"))))
        }

        return posLocalCatalogStore.observeVariationsForProduct(siteModel.localId(), productId)
            .map { result ->
                val variations = result.getOrNull()?.map { it.toWooPosVariation(variationMapper) } ?: emptyList()
                VariationsResult.Remote(Result.success(variations))
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>> =
        withContext(Dispatchers.IO) {
            Result.success(emptyList())
        }

    override fun canLoadMoreVariations(numOfVariations: Int): Boolean = false

    override suspend fun getVariationById(productId: Long, variationId: Long): WooPosVariation? {
        val siteModel = selectedSite.getOrNull() ?: return null
        val result = posLocalCatalogStore.getVariation(
            localSiteId = siteModel.localId(),
            productId = productId,
            variationId = variationId
        )
        return result.getOrNull()?.toWooPosVariation(variationMapper)
    }

    override suspend fun refreshProducts(): Result<WooPosSyncProductResult> = withContext(Dispatchers.IO) {
        selectedSite.getOrNull()?.let { site ->
            val syncResult = localCatalogSyncRepository.syncLocalCatalogIncremental(site)
            Result.success(PosLocalCatalogProductSyncResult(syncResult))
        } ?: Result.failure(IllegalStateException("No site selected"))
    }

    override suspend fun refreshVariations(productId: Long): Result<WooPosSyncVariationResult> = withContext(
        Dispatchers.IO
    ) {
        selectedSite.getOrNull()?.let { site ->
            val syncResult = localCatalogSyncRepository.syncLocalCatalogIncremental(site)
            Result.success(PosLocalCatalogVariationSyncResult(syncResult))
        } ?: Result.failure(IllegalStateException("No site selected"))
    }

    override fun searchProducts(query: String): Flow<SearchProductsResult> = flow {
        val result = localCatalogSearchDataSource.searchProducts(query)

        result.fold(
            onSuccess = { dbSearchResult ->
                emit(
                    SearchProductsResult.Local(
                        products = dbSearchResult.products,
                        searchTimeMillis = dbSearchResult.searchTimeMillis,
                        searchMethod = dbSearchResult.searchMethod,
                    )
                )
            },
            onFailure = { error ->
                emit(SearchProductsResult.Remote(Result.failure(error), 0L))
            }
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun loadMoreSearchResults(query: String): Result<List<WooPosProductModel>> =
        localCatalogSearchDataSource.loadMore(query)

    override val hasMoreSearchPages: Boolean
        get() = localCatalogSearchDataSource.hasMorePages

    companion object {
        private const val PAGE_SIZE = 25
    }
}

class WooPosProductsRemoteDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val productRestClient: ProductRestClient,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val productsIndex: WooPosProductsIndex,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig,
    private val posProductMapper: WooPosWCProductToWooPosProductModelMapper,
    private val variationHandler: VariationListHandler,
    private val variationCache: WooPosVariationsLRUCache,
    private val variationFilterConfig: WooPosVariationsTypesFilterConfig,
    private val variationMapper: WooPosVariationMapper,
    private val searchProductsDataSource: WooPosSearchProductsDataSource,
) : WooPosProductsDataSourceInterface {
    private val canLoadMore = AtomicBoolean(false)
    private val offset = AtomicInteger(0)

    override val hasMoreProductsPages: Boolean
        get() = canLoadMore.get()

    override suspend fun prepopulateCache(): Result<Unit> = coroutineScope {
        productsCache.clear()

        val pageOne = async {
            fetchProductsFromStore(
                offset = 0,
                pageSize = PRE_POPULATION_PAGE_SIZE
            )
        }

        val pageTwo = async {
            fetchProductsFromStore(
                offset = PRE_POPULATION_PAGE_SIZE,
                pageSize = PRE_POPULATION_PAGE_SIZE
            )
        }

        val pageOneResult = pageOne.await()
        val pageTwoResult = pageTwo.await()

        fun List<WCProductModel>?.toAppModels(): List<WooPosProductModel> =
            this?.map { posProductMapper.map(it) } ?: emptyList()

        when {
            pageOneResult.isError -> {
                pageOneResult.logFailure()
                return@coroutineScope Result.failure(WooException(pageOneResult.error))
            }

            pageTwoResult.isError -> {
                pageTwoResult.logFailure()
                productsCache.addAll(pageOneResult.model.toAppModels())
            }

            else -> {
                productsCache.addAll(pageOneResult.model.toAppModels() + pageTwoResult.model.toAppModels())
            }
        }

        Result.success(Unit)
    }

    override fun fetchFirstProductsPage(
        forceRefresh: Boolean
    ): Flow<ProductsResult> = flow {
        offset.set(0)
        productsIndex.clearCache()

        if (!forceRefresh) {
            val cachedProducts = sortProducts(productsCache.getAll()).take(NORMAL_PAGE_SIZE)
            emit(ProductsResult.Cached(cachedProducts))
        }

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            emit(ProductsResult.Remote(Result.success(fetchResult.getOrThrow())))
        } else {
            emit(
                ProductsResult.Remote(
                    Result.failure(
                        fetchResult.exceptionOrNull() ?: Exception("Unknown error")
                    )
                )
            )
        }
    }.flowOn(Dispatchers.IO).take(2)

    override suspend fun loadMoreProducts(): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        if (!canLoadMore.get()) {
            return@withContext Result.success(productsIndex.getProductList())
        }

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            Result.success(fetchResult.getOrThrow())
        } else {
            fetchResult
        }
    }

    private fun sortProducts(products: List<WooPosProductModel>): List<WooPosProductModel> {
        return products.sortedBy { it.name.lowercase() }
    }

    private suspend fun fetchProducts(): Result<List<WooPosProductModel>> {
        val result = fetchProductsFromStore(
            offset = offset.get(),
            pageSize = NORMAL_PAGE_SIZE
        )

        return if (!result.isError) {
            val productsList = result.model ?: emptyList()
            val products = productsList.map { posProductMapper.map(it) }

            canLoadMore.set(productsList.size == NORMAL_PAGE_SIZE)
            offset.addAndGet(NORMAL_PAGE_SIZE)

            productsCache.addAll(products)
            productsIndex.storeProductList(products.map { it.remoteId })
            Result.success(productsIndex.getProductList())
        } else {
            result.logFailure()
            Result.failure(WooException(result.error))
        }
    }

    private suspend fun fetchProductsFromStore(
        offset: Int,
        pageSize: Int
    ): WooResult<List<WCProductModel>> {
        return productStore.fetchProducts(
            site = selectedSite.get(),
            offset = offset,
            pageSize = pageSize,
            filterOptions = productsTypesFilterConfig.filters,
            includeTypes = productsTypesFilterConfig.includeTypes,
            posProductsOnly = true,
        )
    }

    private fun WooResult<*>.logFailure() {
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage")
    }

    override suspend fun getProductById(productId: LocalOrRemoteId.RemoteId): WooPosProductModel? {
        val cachedProduct = productsCache.getProductById(productId.value)
        if (cachedProduct != null) {
            return cachedProduct
        }

        return if (productsCache.getAll().size >= WooPosProductsCache.MAX_CACHE_SIZE) {
            val remoteProductResult = productRestClient.fetchSingleProduct(
                site = selectedSite.get(),
                remoteProductId = productId.value,
            )

            if (!remoteProductResult.isError) {
                val remoteProduct = remoteProductResult.productWithMetaData.product
                val product = posProductMapper.map(remoteProduct)
                productsCache.addAll(listOf(product))
                product
            } else {
                null
            }
        } else {
            null
        }
    }

    override suspend fun resetVariationsListHandler() {
        variationHandler.resetState()
    }

    private suspend fun getCachedVariations(productId: Long): List<WooPosVariation> {
        return variationCache.get(productId) ?: emptyList()
    }

    private suspend fun updateVariationsCache(productId: Long, variations: List<WooPosVariation>) {
        variationCache.put(productId, variations)
    }

    override fun fetchFirstVariationsPage(
        productId: Long,
        forceRefresh: Boolean
    ): Flow<VariationsResult> = flow {
        if (forceRefresh) {
            updateVariationsCache(productId, emptyList())
        }

        val cachedVariations = getCachedVariations(productId)
        if (cachedVariations.isNotEmpty()) {
            emit(VariationsResult.Cached(cachedVariations))
        }

        val result = variationHandler.fetchVariations(
            productId,
            forceRefresh = true,
            filterOptions = variationFilterConfig.filters
        )
        when {
            result.isSuccess -> {
                val remoteVariations = variationHandler.getVariationsFlow(productId).firstOrNull()?.map {
                    it.toWooPosVariation(variationMapper)
                } ?: emptyList()
                updateVariationsCache(productId, remoteVariations)
                emit(VariationsResult.Remote(Result.success(remoteVariations)))
            }

            else -> {
                emit(
                    VariationsResult.Remote(
                        Result.failure(
                            result.exceptionOrNull() ?: Exception("Unknown error while fetching variations")
                        )
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun canLoadMoreVariations(numOfVariations: Int): Boolean {
        return variationHandler.canLoadMore(numOfVariations)
    }

    override suspend fun loadMoreVariations(productId: Long): Result<List<WooPosVariation>> =
        withContext(Dispatchers.IO) {
            val result = variationHandler.loadMore(
                productId,
                filterOptions = variationFilterConfig.filters
            )
            when {
                result.isSuccess -> {
                    val fetchedVariations = variationHandler.getVariationsFlow(
                        productId
                    ).first().map { it.toWooPosVariation(variationMapper) }
                    Result.success(fetchedVariations)
                }

                else -> {
                    result.logVariationFailure()
                    Result.failure(
                        result.exceptionOrNull() ?: Exception("Unknown error while loading more variations")
                    )
                }
            }
        }

    override suspend fun getVariationById(productId: Long, variationId: Long): WooPosVariation? {
        val cachedVariations = getCachedVariations(productId)
        val cachedVariation = cachedVariations.find { it.remoteVariationId == variationId }
        if (cachedVariation != null) {
            return cachedVariation
        }

        return if (cachedVariations.isNotEmpty()) {
            val remoteVariationsResult = productRestClient.fetchProductVariations(
                site = selectedSite.get(),
                productId = productId,
                posProductsOnly = true,
            )

            if (!remoteVariationsResult.isError) {
                val variations = remoteVariationsResult.variations.map { it.toWooPosVariation(variationMapper) }
                updateVariationsCache(productId, variations)
                variations.find { it.remoteVariationId == variationId }
            } else {
                null
            }
        } else {
            null
        }
    }

    override suspend fun refreshProducts(): Result<WooPosSyncProductResult> = withContext(
        Dispatchers.IO
    ) {
        // We can return the last-emitted result which will always be the instance of [ProductsResult.Remote] because
        // when forceRefresh = true the in-memory cache gets wiped out and the flow fetches from remote only.
        val productsResult = fetchFirstProductsPage(forceRefresh = true).last()
        Result.success(productsResult)
    }

    override suspend fun refreshVariations(
        productId: Long
    ): Result<WooPosSyncVariationResult> = withContext(Dispatchers.IO) {
        // We can return the last-emitted result which will always be the instance of [VariationsResult.Remote] because
        // when forceRefresh = true the in-memory cache gets wiped out and the flow fetches from remote only.
        val variationsResult = fetchFirstVariationsPage(productId, forceRefresh = true).last()
        Result.success(variationsResult)
    }

    override fun searchProducts(query: String): Flow<SearchProductsResult> = flow {
        val localProducts = searchProductsDataSource.searchLocalProducts(query)
        if (localProducts.isNotEmpty()) {
            emit(
                SearchProductsResult.Local(
                    products = localProducts,
                    searchTimeMillis = 0L,
                    searchMethod = "in_memory_cache",
                )
            )
        }

        var remoteResult: Result<List<WooPosProductModel>>
        val searchTimeMillis = measureTimeMillis {
            remoteResult = searchProductsDataSource.searchRemoteProducts(query)
        }
        emit(SearchProductsResult.Remote(remoteResult, searchTimeMillis))
    }.flowOn(Dispatchers.IO)

    override suspend fun loadMoreSearchResults(query: String): Result<List<WooPosProductModel>> =
        withContext(Dispatchers.IO) {
            searchProductsDataSource.loadMore(query)
        }

    override val hasMoreSearchPages: Boolean
        get() = searchProductsDataSource.hasMorePages

    companion object {
        private const val NORMAL_PAGE_SIZE = 25
        private const val PRE_POPULATION_PAGE_SIZE = 100
    }
}

private fun Result<Unit>.logVariationFailure() {
    val error = exceptionOrNull()
    val errorMessage = error?.message ?: "Unknown error"
    WooLog.e(WooLog.T.POS, "Loading variations failed - $errorMessage", error)
}
