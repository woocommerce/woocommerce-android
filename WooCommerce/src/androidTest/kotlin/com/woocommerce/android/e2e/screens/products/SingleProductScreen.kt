package com.woocommerce.android.e2e.screens.products

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ComposeUiAutomator
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.allText
import com.woocommerce.android.ui.products.details.ProductDetailTestTags
import com.woocommerce.android.ui.compose.designsystem.R as DesignSystemR

class SingleProductScreen : Screen {
    constructor() : super(R.id.productDetail_root)

    private val composeUi = ComposeUiAutomator()

    fun goBackToProductsScreen(): ProductListScreen {
        // pressBack() only needed if device is not a tablet,
        // on a tablet, products list and product information are on the same screen
        val isTablet = InstrumentationRegistry.getInstrumentation().targetContext
            .resources
            .configuration
            .screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        if (!isTablet) {
            pressBack()
        }
        return ProductListScreen()
    }

    fun assertSingleProductScreen(product: ProductData): SingleProductScreen {
        val topAppBar = composeUi.waitForTag(ProductDetailTestTags.TOP_APP_BAR)
        check(product.name in topAppBar.allText()) {
            "Expected top app bar title '${product.name}', found ${topAppBar.allText()}"
        }

        // Product name:
        val title = composeUi.waitForTag(ProductDetailTestTags.TITLE)
        check(product.name in title.allText()) {
            "Expected product title '${product.name}', found ${title.allText()}"
        }

        // Name-Value pairs:
        assertTextNameValuePair(
            rowKey = "group_${R.string.product_price}",
            name = R.string.product_price,
            valueText = product.price,
        )
        assertTextNameValuePair(
            rowKey = "group_${R.string.product_inventory}",
            name = R.string.product_inventory,
            valueText = "Stock status: ${product.stockStatus}",
        )
        assertTextNameValuePair(
            rowKey = "complex_${R.string.product_type}_${DesignSystemR.drawable.woo_ds_ic_regular_box_24dp}",
            name = R.string.product_type,
            valueText = product.type,
        )

        // Rating is shown only if the rating is larger than zero (more than zero reviews):
        if (product.rating > 0) {
            val reviews = getTranslatedString(R.string.product_reviews)
            composeUi.scrollTextIntoView(ProductDetailTestTags.LIST, reviews)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val ratingDescription = context.getString(
                R.string.product_rating_content_description,
                product.rating.toFloat(),
            )
            assertRowContains(
                rowKey = "rating_${R.string.product_reviews}",
                expectedValues = listOf(reviews, product.getReviewsDescription(), ratingDescription),
            )
        }

        return this
    }

    private fun assertTextNameValuePair(
        rowKey: String,
        @StringRes name: Int,
        valueText: String?,
    ) {
        val nameText = getTranslatedString(name)
        composeUi.scrollTextIntoView(ProductDetailTestTags.LIST, nameText)
        assertRowContains(rowKey, listOfNotNull(nameText, valueText))
    }

    private fun assertRowContains(rowKey: String, expectedValues: List<String>) {
        val row = composeUi.waitForTag(ProductDetailTestTags.row(rowKey), "Product property row '$rowKey'")
        val rowValues = row.allText()
        expectedValues.forEach { expected ->
            check(rowValues.any { expected in it }) {
                "Expected '$expected' in Product Detail row '$rowKey', found $rowValues"
            }
        }
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
