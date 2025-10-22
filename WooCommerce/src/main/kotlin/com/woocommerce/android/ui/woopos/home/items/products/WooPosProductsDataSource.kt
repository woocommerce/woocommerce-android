package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformInstantCatalogFullSync
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(
    private val remoteDataSource: WooPosProductsRemoteDataSource,
    private val localDbDataSource: WooPosProductsInDbDataSource,
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
) {
    private var activeSource: WooPosProductsDataSourceInterface? = null

    fun prepopulateProductsCache(): Flow<WooPosPrepopulatingDataStatus> = flow {
        resetState()
        emit(WooPosPrepopulatingDataStatus.Syncing)

        val requirement = syncStatusChecker.checkSyncRequirement()
        when (requirement) {
            is WooPosFullSyncRequirement.LocalCatalogDisabled -> {
                activeSource = remoteDataSource
                remoteDataSource.prepopulateProductsCache().fold(
                    onSuccess = {
                        emit(WooPosPrepopulatingDataStatus.Completed)
                    },
                    onFailure = {
                        emit(WooPosPrepopulatingDataStatus.Failed(it.message ?: "Unknown error"))
                    }
                )
            }

            is WooPosFullSyncRequirement.NotRequired,
            is WooPosFullSyncRequirement.Overdue -> {
                activeSource = localDbDataSource
                emit(WooPosPrepopulatingDataStatus.Completed)
            }

            is WooPosFullSyncRequirement.BlockingRequired -> {
                activeSource = localDbDataSource
                localDbDataSource.prepopulateProductsCache().fold(
                    onSuccess = {
                        emit(WooPosPrepopulatingDataStatus.Completed)
                    },
                    onFailure = {
                        emit(WooPosPrepopulatingDataStatus.Failed(it.message ?: "Unknown error"))
                    }
                )
            }

            is WooPosFullSyncRequirement.Error -> {
                emit(WooPosPrepopulatingDataStatus.Failed(requirement.message))
            }
        }
    }

    fun fetchFirstPage(forceRefresh: Boolean): Flow<ProductsResult> =
        activeSource?.fetchFirstPage(forceRefresh)
            ?: error("FetchFirstPage - Data source not selected")

    suspend fun loadMore(): Result<List<WooPosProductModel>> {
        return activeSource?.loadMore() ?: error("LoadMore - Data source not selected")
    }

    val hasMorePages: Boolean
        get() = activeSource?.hasMorePages ?: error("hasMorePages - Data source not selected")

    suspend fun resetState() {
        activeSource = null
        remoteDataSource.resetState()
        localDbDataSource.resetState()
    }

    sealed class ProductsResult {
        data class Cached(val products: List<WooPosProductModel>) : ProductsResult()
        data class Remote(val productsResult: Result<List<WooPosProductModel>>) : ProductsResult()
    }

    sealed class WooPosPrepopulatingDataStatus {
        data object Syncing : WooPosPrepopulatingDataStatus()
        data object Completed : WooPosPrepopulatingDataStatus()
        data class Failed(val error: String) : WooPosPrepopulatingDataStatus()
    }
}

class WooPosProductsInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val mapper: WooPosProductModelMapper,
    private val performInstantCatalogFullSync: WooPosPerformInstantCatalogFullSync,
) : WooPosProductsDataSourceInterface {

    private fun getProductsFromDatabaseFlow(): Flow<List<WooPosProductModel>> {
        val siteModel = selectedSite.getOrNull() ?: return flow { emit(emptyList()) }
        val siteId = org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId(siteModel.id)

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

    override suspend fun prepopulateProductsCache(): Result<Unit> = performInstantCatalogFullSync()
}

class WooPosProductsRemoteDataSource @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val productsIndex: WooPosProductsIndex,
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig,
    private val posProductMapper: WooPosWCProductToWooPosProductModelMapper,
) : WooPosProductsDataSourceInterface {
    private val canLoadMore = AtomicBoolean(false)
    private val offset = AtomicInteger(0)

    override val hasMorePages: Boolean
        get() = canLoadMore.get()

    override suspend fun prepopulateProductsCache(): Result<Unit> = coroutineScope {
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

    override fun fetchFirstPage(
        forceRefresh: Boolean
    ): Flow<WooPosProductsDataSource.ProductsResult> = flow {
        offset.set(0)
        productsIndex.clearCache()

        if (!forceRefresh) {
            val cachedProducts = sortProducts(productsCache.getAll()).take(NORMAL_PAGE_SIZE)
            emit(WooPosProductsDataSource.ProductsResult.Cached(cachedProducts))
        }

        val fetchResult = fetchProducts()

        if (fetchResult.isSuccess) {
            emit(WooPosProductsDataSource.ProductsResult.Remote(Result.success(fetchResult.getOrThrow())))
        } else {
            emit(
                WooPosProductsDataSource.ProductsResult.Remote(
                    Result.failure(
                        fetchResult.exceptionOrNull() ?: Exception("Unknown error")
                    )
                )
            )
        }
    }.flowOn(Dispatchers.IO).take(2)

    override suspend fun loadMore(): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
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
        )
    }

    private fun WooResult<*>.logFailure() {
        val errorMessage = error?.message ?: "Unknown error"
        WooLog.e(WooLog.T.POS, "Loading products failed - $errorMessage")
    }

    override suspend fun resetState() {
        canLoadMore.set(false)
        offset.set(0)
    }

    companion object {
        private const val NORMAL_PAGE_SIZE = 25
        private const val PRE_POPULATION_PAGE_SIZE = 100
    }
}
