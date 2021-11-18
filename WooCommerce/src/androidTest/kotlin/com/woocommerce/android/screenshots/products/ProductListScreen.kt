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

    fun assertProductCardMatchesMockData(product: ProductData): ProductListScreen {
        // check the card information against the json data

        // matcher for the card which has the correct product name
        val matcherForProductCard = Matchers.allOf(
            // must have element id
            ViewMatchers.withId(R.id.productInfoContainer),
            // must have a child
            ViewMatchers.withChild(
                Matchers.allOf(
                    // child must have id productName
                    ViewMatchers.withId(R.id.productName),
                    // child text must match the product name mock data
                    ViewMatchers.withText(product.name),
                )
            )
        )
        val productCardMatch = Espresso.onView(matcherForProductCard)
        productCardMatch.check(
            // check if it exists!
            ViewAssertions.matches(ViewMatchers.withId(R.id.productInfoContainer))
        )
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
