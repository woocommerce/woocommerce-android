package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("MagicNumber")
class WooPosItemsSearchEmptyStateProvider @Inject constructor(
    private val productsMockedDataSource: WooPosSearchProductsDataSource,
) {
    suspend fun getPopularItems(): List<Product> {
        delay(50)
        return productsMockedDataSource.getPopularProducts()
    }

    suspend fun getLastSearches(): List<String> {
        delay(70)
        return listOf(
            "T-shirt",
            "Jeans",
            "Shoes",
        )
    }
}
