package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class WooPosSearchResultsCache @Inject constructor(
    private val productsCache: WooPosProductsCache
) {
    private val mutex = Mutex()

    // Store product IDs for the current search query's pages
    private var currentSearchQuery: String = ""
    private var paginatedResults: List<Long> = emptyList()

    suspend fun storeSearchResults(query: String, productIds: List<Long>) = mutex.withLock {
        // If the query changes, clear previous pagination results
        if (currentSearchQuery != query.lowercase()) {
            paginatedResults = emptyList()
            currentSearchQuery = query.lowercase()
        }

        // Add potential next page of search results to the list
        paginatedResults = (paginatedResults + productIds).distinct()
    }

    suspend fun getSearchResults(query: String): List<Product> = mutex.withLock {
        if (currentSearchQuery != query.lowercase()) {
            return emptyList()
        }
        return paginatedResults.mapNotNull { productsCache.getProductById(it) }
    }

    suspend fun hasSearchResults(query: String): Boolean = mutex.withLock {
        return currentSearchQuery == query.lowercase() && paginatedResults.isNotEmpty()
    }

    suspend fun clearCache() = mutex.withLock {
        currentSearchQuery = ""
        paginatedResults = emptyList()
    }
}
