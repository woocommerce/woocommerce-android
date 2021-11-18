package com.woocommerce.android.screenshots.products

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
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
        Espresso.onView(ViewMatchers.withId(LIST_VIEW)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        productTitle
                    )
                ),
                ViewActions.scrollTo()
            )
        )
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
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }

    fun selectSingleProductByName(productName: String): SingleProductScreen {
        Espresso.onView(ViewMatchers.withId(LIST_VIEW)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(
                    ViewMatchers.withText(
                        productName
                    )
                ),
                ViewActions.click()
            )
        )

        return SingleProductScreen()
    }
}
