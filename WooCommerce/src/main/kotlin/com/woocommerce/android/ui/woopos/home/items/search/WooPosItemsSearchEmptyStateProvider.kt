package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("MagicNumber")
class WooPosItemsSearchEmptyStateProvider @Inject constructor() {
    suspend fun getPopularItems(): List<WooPosItemSelectionViewState.Product> {
        delay(50)
        return listOf(
            WooPosItemSelectionViewState.Product.Simple(
                id = 1,
                name = "Popular Item 1",
                price = "10.0$",
                imageUrl = "https://example.com/image1.jpg",
            ),
            WooPosItemSelectionViewState.Product.Simple(
                id = 2,
                name = "Popular Item 2",
                price = "20.0$",
                imageUrl = "https://example.com/image2.jpg",
            ),
            WooPosItemSelectionViewState.Product.Variable(
                id = 3,
                name = "Popular Item 3",
                price = "30.0$",
                imageUrl = "https://example.com/image3.jpg",
                numOfVariations = 2,
                variationIds = listOf(4, 5),
            ),
        )
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
