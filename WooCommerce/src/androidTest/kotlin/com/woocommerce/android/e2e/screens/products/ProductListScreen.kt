package com.woocommerce.android.e2e.screens.products

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.e2e.helpers.util.CustomMatchers
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import com.woocommerce.android.ui.products.ProductItemView
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf

class ProductListScreen : Screen {
    constructor() : super(R.id.productsRecycler)

    fun scrollToProduct(productTitle: String): ProductListScreen {
        scrollToListItem(productTitle, R.id.productsRecycler)
        return ProductListScreen()
    }

    fun selectProductByName(productName: String): SingleProductScreen {
        selectListItem(productName, R.id.productsRecycler)
        waitForElementToBeDisplayed(R.id.productDetail_root)
        return SingleProductScreen()
    }

    fun tapOnCreateProduct(): ProductListScreen {
        clickOn(R.id.addProductButton)
        return this
    }

    fun goBackToProductList(): ProductListScreen {
        while (!isElementDisplayed(R.id.productsRecycler)) {
            pressBack()
        }

        return this
    }

    fun openSearchPane(): ProductListScreen {
        if (!Screen.isElementFocused(androidx.appcompat.R.id.search_src_text)) {
            clickOn(R.id.menu_search)
        }
        return this
    }

    fun tapSearchAllProducts(): ProductListScreen {
        selectItemWithTitleInTabLayout(R.string.product_search_all, R.id.productsSearchTabView)
        return this
    }

    fun tapSearchSKU(): ProductListScreen {
        selectItemWithTitleInTabLayout(R.string.product_search_sku, R.id.productsSearchTabView)
        return this
    }

    fun enterSearchTerm(term: String): ProductListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        idleFor(1000) // allow for UI transitions
        waitForAtLeastOneElementToBeDisplayed(R.id.productInfoContainer)
        return this
    }

    fun enterAbsentSearchTerm(term: String): ProductListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        // If we don't expect for results, we wait for "no results" situation
        waitForElementToBeDisplayed(R.id.empty_view_title)
        return this
    }

    fun tapFilters(): FilterScreen {
        clickOn(R.id.btn_product_filter)
        return FilterScreen()
    }

    fun tapSort(): ProductListScreen {
        clickOn(R.id.btn_product_sorting)
        return this
    }

    fun selectSortOption(sortOption: String): ProductListScreen {
        clickByTextAndId(sortOption, R.id.sortingItem_name)
        return this
    }

    fun assertProductIsAtPosition(productName: String, position: Int): ProductListScreen {
        Espresso.onView(
            withRecyclerView(R.id.productsRecycler).atPositionOnView(position, R.id.productName)
        )
            .check(matches(ViewMatchers.withText(productName)))

        return this
    }

    fun leaveOrClearSearchMode(): ProductListScreen {
        // to support test on tablets - search bar is displayed on split screen
        // clearing search bar so test can continue in a clean state
        if (Screen.isElementDisplayed(R.id.productDetailsErrorImage)) {
            clearSearchBar(androidx.appcompat.R.id.search_src_text)
        } // to support test on phones
        else if (Screen.isElementDisplayed(androidx.appcompat.R.id.search_src_text)) {
            // Double pressBack is needed because first one only removes the focus
            // from search field, while the second one leaves the search mode.
            pressBack()
            pressBack()
        }
        return this
    }

    fun leaveSearchMode(): ProductListScreen {
        var isProductDetailsErrorDisplayed = Screen.isElementDisplayed(R.id.productDetailsErrorImage)
        var isSearchTextBarDisplayed = Screen.isElementDisplayed(androidx.appcompat.R.id.search_src_text)

        if (isProductDetailsErrorDisplayed && isSearchTextBarDisplayed) {
            clearSearchBar(androidx.appcompat.R.id.search_src_text)

            // this is to click the back button on search bar to go back to products list
            // using the content description matcher as there isn't an ID for the button
            Espresso.onView(
                allOf(
                    Matchers.allOf(
                        ViewMatchers.withContentDescription("Collapse"),
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                    )
                )
            ).perform(click())
        } else if (isSearchTextBarDisplayed) {
            // Double pressBack is needed because first one only removes the focus
            // from search field, while the second one leaves the search mode.
            pressBack()
            pressBack()
        }
        return this
    }

    fun assertProductCard(product: ProductData): ProductListScreen {
        // If a product has an SKU, value will be prefixed with "SKU :" on screen.
        // If a product has no SKU, the field won't be shown at all.
        val expectedSKU = if (product.sku.isEmpty()) "" else "SKU: ${product.sku}"

        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.productInfoContainer),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.productName),
                        ViewMatchers.withText(product.name)
                    )
                ),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.productStockAndStatus),
                        ViewMatchers.withText(
                            Matchers.containsString("${product.stockStatus}${product.variations} • ")
                        ),
                        ViewMatchers.withText(Matchers.containsString(product.priceDiscountedRaw))
                    )
                ),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.productSku),
                        ViewMatchers.withText(expectedSKU)
                    )
                )
            )
        )
            .check(matches(ViewMatchers.isDisplayed()))

        return this
    }

    fun assertProductsCount(count: Int): ProductListScreen {
        Espresso.onView(
            ViewMatchers.withId(R.id.productsRecycler)
        )
            .check(matches(CustomMatchers().withViewCount(instanceOf(ProductItemView::class.java), count)))

        return this
    }
}
