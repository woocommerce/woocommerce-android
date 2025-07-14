package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WooPosItemsSearchEmptyStateRepository @Inject constructor(
    private val preferencesRepository: WooPosPreferencesRepository,
    private val popularProductsProvider: WooPosPopularProductsProvider,
) {
    suspend fun getPopularItems(): List<Product> = popularProductsProvider.getPopularProducts()

    suspend fun getLastSearches(): List<String> = preferencesRepository.recentProductSearches.first()

    suspend fun addRecentSearch(search: String) {
        preferencesRepository.addRecentProductSearch(search)
    }

    suspend fun addPopularItemsToCache() {
        popularProductsProvider.addPopularItemsToCache()
    }
}
