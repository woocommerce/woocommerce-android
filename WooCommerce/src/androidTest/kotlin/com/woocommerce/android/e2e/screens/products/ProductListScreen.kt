package com.woocommerce.android.e2e.screens.products

import androidx.test.espresso.Espresso
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
import org.hamcrest.Matchers.instanceOf

class ProductListScreen : Screen {
    companion object {
        const val LIST_VIEW = R.id.productsRecycler
    }

    constructor() : super(LIST_VIEW)

    fun scrollToProduct(productTitle: String): ProductListScreen {
        scrollToListItem(productTitle, LIST_VIEW)
        return ProductListScreen()
    }

    fun selectProductByName(productName: String): SingleProductScreen {
        selectListItem(productName, LIST_VIEW)
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
        clickOn(R.id.menu_search)
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
        // Sleep to let the previous results disappear
        Thread.sleep(2000)
        waitForAtLeastOneElementToBeDisplayed(R.id.productInfoContainer)
        return this
    }

    fun enterAbsentSearchTerm(term: String): ProductListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        // Sleep to let the previous results disappear
        Thread.sleep(2000)
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
            withRecyclerView(LIST_VIEW).atPositionOnView(position, R.id.productName)
        )
            .check(matches(ViewMatchers.withText(productName)))

        return this
    }

    fun leaveSearchMode(): ProductListScreen {
        if (Screen.isElementDisplayed(androidx.appcompat.R.id.search_src_text)) {
            // Double pressBack is needed because first one only removes the focus
            // from search field, while the second one leaves the search mode.
            Espresso.pressBack()
            Espresso.pressBack()
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
                            "${product.stockStatus}${product.variations} • \$${product.priceDiscountedRaw}.00"

                        )
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
