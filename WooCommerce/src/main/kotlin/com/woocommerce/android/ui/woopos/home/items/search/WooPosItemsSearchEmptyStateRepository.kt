package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Suppress("MagicNumber")
class WooPosItemsSearchEmptyStateRepository @Inject constructor(
    private val productsDataSource: WooPosSearchProductsDataSource,
    private val preferencesRepository: WooPosPreferencesRepository
) {
    suspend fun getPopularItems(): List<Product> {
        delay(50)
        return productsDataSource.getPopularProducts()
    }

    suspend fun getLastSearches(): List<String> = preferencesRepository.recentProductSearches.first()

    suspend fun addRecentSearch(search: String) {
        preferencesRepository.addRecentProductSearch(search)
    }
}
