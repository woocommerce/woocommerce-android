package com.woocommerce.android.e2e.tests.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductSelectorScreen
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ListItem.ProductListItem
import com.woocommerce.android.ui.products.selector.SelectionState
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ProductSelectorScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenPopularProducts_displayPopularProductsHeading() {
        rule.setContent {
            ProductSelectorScreen(
                state = ProductSelectorViewModel.ViewState(
                    loadingState = ProductSelectorViewModel.LoadingState.IDLE,
                    products = generateProductList(),
                    popularProducts = generateProductList(),
                    recentProducts = emptyList(),
                    selectedItemsCount = 0,
                    filterState = ProductSelectorViewModel.FilterState(emptyMap(), null),
                    searchState = ProductSelectorViewModel.SearchState.EMPTY
                ),
                onDoneButtonClick = {},
                onClearButtonClick = {},
                onFilterButtonClick = {},
                onProductClick = { _, _ -> },
                onLoadMore = {},
                onSearchQueryChanged = {},
                onSearchTypeChanged = {},
                onClearFiltersButtonClick = {},
                trackConfigurableProduct = {}
            )
        }

        rule.onNodeWithText(rule.activity.getString(R.string.product_selector_popular_products_heading)).assertExists()
    }

    @Test
    fun givenNoPopularProducts_DoNotDisplayPopularProductsHeading() {
        rule.setContent {
            ProductSelectorScreen(
                state = ProductSelectorViewModel.ViewState(
                    loadingState = ProductSelectorViewModel.LoadingState.IDLE,
                    products = generateProductList(),
                    popularProducts = emptyList(),
                    recentProducts = emptyList(),
                    selectedItemsCount = 0,
                    filterState = ProductSelectorViewModel.FilterState(emptyMap(), null),
                    searchState = ProductSelectorViewModel.SearchState.EMPTY
                ),
                onDoneButtonClick = {},
                onClearButtonClick = {},
                onFilterButtonClick = {},
                onProductClick = { _, _ -> },
                onLoadMore = {},
                onSearchQueryChanged = {},
                onSearchTypeChanged = {},
                onClearFiltersButtonClick = {},
                trackConfigurableProduct = {}
            )
        }

        rule.onNodeWithText(
            rule.activity.getString(R.string.product_selector_popular_products_heading)
        ).assertDoesNotExist()
    }

    private fun generateProductList(): List<ProductSelectorViewModel.ListItem> {
        return listOf(
            ProductListItem(
                productId = 1,
                title = "Product 1",
                type = ProductType.SIMPLE,
                imageUrl = null,
                numVariations = 0,
                stockAndPrice = "Not in stock • $25.00",
                sku = "1234",
                selectionState = SelectionState.SELECTED
            ),

            ProductListItem(
                productId = 2,
                title = "Product 2",
                type = ProductType.VARIABLE,
                imageUrl = null,
                numVariations = 3,
                stockAndPrice = "In stock • $5.00 • 3 variations",
                sku = "33333",
                selectionState = SelectionState.PARTIALLY_SELECTED
            ),

            ProductListItem(
                productId = 3,
                title = "Product 3",
                type = ProductType.GROUPED,
                imageUrl = "",
                numVariations = 0,
                stockAndPrice = "Out of stock",
                sku = null
            ),

            ProductListItem(
                productId = 4,
                title = "Product 4",
                type = ProductType.GROUPED,
                imageUrl = null,
                numVariations = 0,
                stockAndPrice = null,
                sku = null
            )
        )
    }
}
