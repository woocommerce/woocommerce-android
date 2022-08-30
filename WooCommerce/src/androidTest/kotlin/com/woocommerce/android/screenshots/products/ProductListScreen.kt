package com.woocommerce.android.screenshots.products

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.ProductData
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

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
                        ViewMatchers.withText(product.stockStatus + " • \$${product.priceDiscountedRaw}.00")
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }
}
