package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosFtsSearchResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsSearchInDbDataSource @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val productMapper: WooPosProductModelMapper,
    private val logger: WooPosLogWrapper,
) {
    companion object {
        private const val PAGE_SIZE = 15
    }

    data class DbSearchResult(
        val products: List<WooPosProductModel>,
        val searchTimeMillis: Long,
        val searchMethod: String,
    )

    private val searchOffset = AtomicInteger(0)
    private val canLoadMoreResults = AtomicBoolean(false)
    private var currentQuery: String = ""
    private val accumulatedResults = mutableListOf<WooPosProductModel>()

    val hasMorePages: Boolean
        get() = canLoadMoreResults.get()

    suspend fun searchProducts(query: String): Result<DbSearchResult> = withContext(Dispatchers.IO) {
        searchOffset.set(0)
        currentQuery = query
        accumulatedResults.clear()
        performSearch(query)
    }

    suspend fun loadMore(query: String): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        if (!canLoadMoreResults.get() || query != currentQuery) {
            return@withContext Result.success(accumulatedResults.toList())
        }

        performSearch(query).map { it.products }
    }

    private suspend fun performSearch(query: String): Result<DbSearchResult> {
        val siteModel = selectedSite.getOrNull() ?: return Result.failure(
            IllegalStateException("No site selected")
        )
        val siteId = LocalOrRemoteId.LocalId(siteModel.id)

        val startTime = System.currentTimeMillis()
        val offset = searchOffset.get()
        logger.d("performFtsSearch: query=\"$query\", offset=$offset, pageSize=$PAGE_SIZE")

        val result = posLocalCatalogStore.searchProductsFts(
            siteId = siteId,
            searchQuery = query,
            pageSize = PAGE_SIZE,
            offset = offset
        )

        return result.fold(
            onSuccess = { ftsResults ->
                val mappedProducts = ftsResults.map { ftsResult ->
                    when (ftsResult) {
                        is WooPosFtsSearchResult.Product -> productMapper.fromEntity(ftsResult.entity)
                        is WooPosFtsSearchResult.Variation -> productMapper.fromVariationEntity(
                            entity = ftsResult.entity,
                            parentProductName = ftsResult.parentProductName,
                        )
                    }
                }

                accumulatedResults.addAll(mappedProducts)
                canLoadMoreResults.set(ftsResults.size == PAGE_SIZE)
                searchOffset.addAndGet(PAGE_SIZE)

                val duration = System.currentTimeMillis() - startTime
                logger.d(
                    "performFtsSearch completed: ${ftsResults.size} results " +
                        "(${accumulatedResults.size} total). Duration: ${duration}ms"
                )

                Result.success(
                    DbSearchResult(
                        products = accumulatedResults.toList(),
                        searchTimeMillis = duration,
                        searchMethod = "fts",
                    )
                )
            },
            onFailure = { error ->
                val duration = System.currentTimeMillis() - startTime
                logger.e("performFtsSearch failed after ${duration}ms: ${error.message}", error)
                Result.failure(error)
            }
        )
    }
}
