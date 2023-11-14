package com.woocommerce.android.e2e.screens.products

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.CustomMatchers
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import org.hamcrest.Matchers

class SingleProductScreen : Screen {
    constructor() : super(R.id.productDetail_root)

    fun goBackToProductsScreen(): ProductListScreen {
        pressBack()
        waitForElementToBeDisplayed(R.id.productsRecycler)
        return ProductListScreen()
    }

    fun assertSingleProductScreen(product: ProductData): SingleProductScreen {
        // Navigation bar:
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar),
                ViewMatchers.withChild(ViewMatchers.withText(product.name))
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Product name:
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.editText), ViewMatchers.withText(product.name)
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Name-Value pairs:
        assertTextNameValuePair("Price", product.price)
        assertTextNameValuePair("Inventory", "Stock status: ${product.stockStatus}")
        assertTextNameValuePair("Product type", product.type)

        // Rating is shown only if the rating is larger than zero (more than zero reviews):
        if (product.rating > 0) {
            // Check that "Review" label, actual rating (stars) and reviews count are
            // all direct children of the same container:

            Espresso.onView(
                Matchers.allOf(
                    ViewMatchers.withChild(
                        Matchers.allOf(
                            ViewMatchers.withId(R.id.textPropertyName),
                            ViewMatchers.withText(R.string.product_reviews)
                        )
                    ),
                    ViewMatchers.withChild(
                        Matchers.allOf(
                            ViewMatchers.withId(R.id.ratingBar),
                            CustomMatchers().withStarsNumber(product.rating)
                        )
                    ),
                    ViewMatchers.withChild(
                        Matchers.allOf(
                            ViewMatchers.withId(R.id.textPropertyValue),
                            ViewMatchers.withText(product.getReviewsDescription())
                        )
                    )
                )
            ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }

        return this
    }

    // Checks that label and actual value are siblings in view hierarchy:
    private fun assertTextNameValuePair(nameText: String, valueText: String?) {
        // Scroll the object into visible area
        scrollToListItem(
            nameText,
            Espresso.onView(
                Matchers.allOf(
                    ViewMatchers.withId(R.id.propertiesRecyclerView),
                    ViewMatchers.hasDescendant(ViewMatchers.withText(nameText))
                )
            )
        )

        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.textPropertyName), ViewMatchers.withText(nameText)
                    )
                ),
                ViewMatchers.withChild(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.textPropertyValue), ViewMatchers.withText(valueText)
                    )
                )
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun ProductData.getReviewsDescription(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return when (this.reviewsCount) {
            0 -> context.getString(R.string.product_ratings_count_zero)
            1 -> context.getString(R.string.product_ratings_count_one)
            else -> context.getString(R.string.product_ratings_count, this.reviewsCount)
        }
    }
}
