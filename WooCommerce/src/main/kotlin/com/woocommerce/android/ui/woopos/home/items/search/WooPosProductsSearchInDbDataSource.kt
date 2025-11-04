package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
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
) {
    companion object {
        private const val PAGE_SIZE = 15
    }

    private val searchOffset = AtomicInteger(0)
    private val canLoadMoreResults = AtomicBoolean(false)
    private var currentQuery: String = ""
    private val accumulatedResults = mutableListOf<WooPosProductModel>()

    val hasMorePages: Boolean
        get() = canLoadMoreResults.get()

    suspend fun searchProducts(query: String): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        searchOffset.set(0)
        currentQuery = query
        accumulatedResults.clear()
        performSearch(query)
    }

    suspend fun loadMore(query: String): Result<List<WooPosProductModel>> = withContext(Dispatchers.IO) {
        if (!canLoadMoreResults.get() || query != currentQuery) {
            return@withContext Result.success(accumulatedResults.toList())
        }

        performSearch(query)
    }

    private suspend fun performSearch(query: String): Result<List<WooPosProductModel>> {
        val siteModel = selectedSite.getOrNull() ?: return Result.failure(
            IllegalStateException("No site selected")
        )
        val siteId = LocalOrRemoteId.LocalId(siteModel.id)

        val result = posLocalCatalogStore.searchProducts(
            siteId = siteId,
            searchQuery = query,
            pageSize = PAGE_SIZE,
            offset = searchOffset.get()
        )

        return result.fold(
            onSuccess = { products ->
                val mappedProducts = products.map { entity ->
                    productMapper.fromEntity(entity)
                }.sortedBy { it.name.lowercase() }

                accumulatedResults.addAll(mappedProducts)
                canLoadMoreResults.set(products.size == PAGE_SIZE)
                searchOffset.addAndGet(PAGE_SIZE)

                Result.success(accumulatedResults)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
