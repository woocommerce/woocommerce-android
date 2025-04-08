package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosMockedPopularProductsProvider
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("MagicNumber")
class WooPosItemsSearchEmptyStateProvider @Inject constructor(
    private val popularProductsProvider: WooPosMockedPopularProductsProvider,
) {
    suspend fun getPopularItems(): List<Product> {
        delay(50)
        return popularProductsProvider.getPopularProducts()
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
