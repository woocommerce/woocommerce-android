@file:Suppress("DEPRECATION")

package com.woocommerce.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.util.MocksReader
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.iterator
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooPosProductScreenTest : TestBase() {
    @get:Rule(order = 1)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 3)
    val initRule = InitializationRule()

    @get:Rule(order = 4)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 5)
    var retryTestRule = RetryTestRule()

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() = runTest {
        rule.inject()
        dataStore.edit { it.clear() }
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent()
            .gotoMoreMenuScreen()
            .openPOSScreen(composeTestRule)
    }

    @Test
    fun testProductScreenIsDisplayedWhenPosModeEntered() = runTest {
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
    }

    @Test
    fun testCartItemIsAddedWhenProductItemClicked() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        // asserting the cart is empty initially
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item${firstProduct.name}")
                .assertDoesNotExist()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }
    }

    @Test
    fun testCheckoutButtonIsNotVisibleWhenCartListIsNotDisplayed() = runTest {
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        // asserting the cart is empty
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsNotDisplayed()
            true
        }
    }

    @Suppress("LongMethod")
    @Test
    fun testCheckoutButtonVisibility() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        // asserting the checkout button is not displayed when the cart is empty
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }

        // asserting the checkout button is displayed when the cart has items
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_close_icon_${firstProduct.name}")
                .performClick()
            true
        }

        // asserting the checkout button is not displayed when the cart items are removed
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .performClick()
            true
        }

        // asserting the checkout button is not displayed when we move to the checkout screen
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsNotDisplayed()
            true
        }
    }

    @Suppress("LongMethod")
    @Test
    fun testCartItemRemovalFunctionality() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        val secondProduct = products[1]
        val thirdProduct = products[2]
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${secondProduct.name}").performClick()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${thirdProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${secondProduct.name}")
                .assertExists()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${thirdProduct.name}")
                .assertExists()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_close_icon_${secondProduct.name}")
                .performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${secondProduct.name}")
                .isNotDisplayed()
            true
        }
    }

    @Test
    fun testClickingCheckoutButtonHidesProductsScreen() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("product_list")
                .assertIsNotDisplayed()
            true
        }
    }

    @Test
    fun testClickingCheckoutButtonDisplaysTotalsScreen() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_totals_loaded_screen")
                .assertIsDisplayed()
            true
        }
    }

    @Test
    fun testClickingCheckoutButtonDisplaysCollectPaymentButton() = runTest {
        val productsJSONArray = MocksReader().readAllProductsToArray()
        val products = mutableListOf<ProductData>()
        for (productJSON in productsJSONArray.iterator()) {
            products.add(mapJSONToProduct(productJSON))
        }
        val firstProduct = products.first()
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                e.printStackTrace()
                false
            }
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_list")
                .assertIsNotDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_product_item${firstProduct.name}").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_cart_item_${firstProduct.name}")
                .assertExists()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .assertIsDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_checkout_button")
                .performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_totals_loaded_screen")
                .assertIsDisplayed()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText(
                composeTestRule.activity.getString(
                    R.string.woopos_payment_collect_payment_label
                )
            )
                .assertIsDisplayed()
            true
        }
    }

    private fun mapJSONToProduct(productJSON: JSONObject): ProductData {
        return ProductData(
            id = productJSON.getInt("id"),
            name = productJSON.getString("name"),
            stockStatusRaw = productJSON.getString("stock_status"),
            priceDiscountedRaw = productJSON.getString("price"),
            priceRegularRaw = productJSON.getString("regular_price"),
            typeRaw = productJSON.getString("type"),
            rating = productJSON.getInt("average_rating"),
            reviewsCount = productJSON.getInt("rating_count")
        )
    }
}
