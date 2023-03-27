package com.woocommerce.android.e2e.screens.products

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.util.TreeIterables
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.ui.products.ProductItemView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.TypeSafeMatcher

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
        typeTextInto(R.id.search_src_text, term)
        Thread.sleep(2000)
        return this
    }

    fun leaveSearchMode(): ProductListScreen {
        if (Screen.isElementDisplayed(R.id.search_src_text)) {
            Espresso.pressBack()
            Espresso.pressBack()
        }
        return this
    }

    fun assertProductCard(product: ProductData): ProductListScreen {
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
                            product.stockStatus + product.variations + " • \$${product.priceDiscountedRaw}.00"
                        )
                    )
                ),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.productSku),
                        // Check for SKU value is disabled because of
                        // https://github.com/woocommerce/woocommerce-android/issues/8663
                        // ViewMatchers.withText(product.sku)
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }

    fun assertProductsCount(count: Int): ProductListScreen {
        Espresso.onView(
            ViewMatchers.withId(R.id.productsRecycler)
        )
            .check(matches(withViewCount(instanceOf(ProductItemView::class.java), count)))

        return this
    }

    // Hat tip https://stackoverflow.com/a/69943394
    private fun withViewCount(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            private var actualCount = -1
            override fun describeTo(description: Description) {
                when {
                    actualCount >= 0 -> description.also {
                        it.appendText("Expected items count: $expectedCount, but got: $actualCount")
                    }
                }
            }

            override fun matchesSafely(root: View?): Boolean {
                actualCount = TreeIterables.breadthFirstViewTraversal(root).count {
                    viewMatcher.matches(it)
                }
                return expectedCount == actualCount
            }
        }
    }
}
