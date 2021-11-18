package com.woocommerce.android.ui.main

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.util.ProductData
import com.woocommerce.android.screenshots.util.MocksReader
import com.woocommerce.android.screenshots.util.iterator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ProductsUITest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent().gotoProductsScreen()
    }

    private fun mapJSONToProduct(productJSON: JSONObject): ProductData {
        return ProductData(
            id = productJSON.getInt("id"),
            name = productJSON.getString("name"),
        )
    }

    @Test
    fun productListShowsAllProducts() {
        val productsJSONArray = MocksReader().readAllProductsToArray()

        for (productJSON in productsJSONArray.iterator()) {
            // parsing the json data
            val productData = mapJSONToProduct(productJSON)

            // create the screen ui object and select the product from the list by name
            val productListScreen = ProductListScreen()
                .scrollToProduct(productData.name)
                .assertProductCard(productData)

            // for that detail view, check the data against the UI elements.
            productListScreen.selectSingleProductByName(productData.name)
                .assertProductDetails(productData)
                .goBackToProductsScreen()
        }
    }
}
