@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.useMockedAPI
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.products.ProductListScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ProductsRealAPI : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            useMockedAPI = false
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            useMockedAPI = true
        }
    }

    @Before
    fun setUp() {
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.E2E_REAL_API_URL)
            .proceedWith(BuildConfig.E2E_REAL_API_EMAIL)
            .proceedWith(BuildConfig.E2E_REAL_API_PASSWORD)

        TabNavComponent()
            .gotoProductsScreen()
    }

    @After
    fun tearDown() {
        ProductListScreen()
            .leaveSearchMode()

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
    }

    private val productSalad = ProductData(
        name = "Chicken Teriyaki Salad",
        stockStatusRaw = "instock",
        priceDiscountedRaw = "7",
        sku = "SLD-CHK-TRK"
    )

    private val productCappuccino = ProductData(
        name = "Cappuccino",
        stockStatusRaw = "instock",
        variations = " • 6 variations",
        priceDiscountedRaw = "2",
        sku = "CF-CPC"
    )

    private val productCappuccinoAlmondMedium = ProductData(
        name = "Cappuccino",
        stockStatusRaw = "instock",
        priceDiscountedRaw = "3",
        sku = "CF-CPC-ALM-M"
    )

    private val productCappuccinoAlmondLarge = ProductData(
        name = "Cappuccino",
        stockStatusRaw = "instock",
        priceDiscountedRaw = "4",
        sku = "CF-CPC-ALM-L"
    )

    @Test
    fun e2eRealApiProductsSearchUsual() {
        ProductListScreen()
            // Make sure all products are listed
            .assertProductCard(productCappuccino)
            .assertProductCard(productSalad)
            .assertProductsCount(2)
            // Start search
            .openSearchPane()
            .tapSearchAllProducts()
            // Search for 'productCappuccino'
            .enterSearchTerm(productCappuccino.name)
            .assertProductCard(productCappuccino)
            .assertProductsCount(1)
            // Search for 'productSalad'
            .enterSearchTerm(productSalad.name)
            .assertProductCard(productSalad)
            .assertProductsCount(1)
            // Search for non-existing product
            .enterSearchTerm("Unexisting Product")
            .assertProductsCount(0)
            // Leave search and make sure all products are listed
            .leaveSearchMode()
            .assertProductCard(productCappuccino)
            .assertProductCard(productSalad)
            .assertProductsCount(2)
    }

    @Test
    fun e2eRealApiProductsSearchBySKU() {
        ProductListScreen()
            .openSearchPane()
            .tapSearchSKU()
            // Search for a simple product SKU
            .enterSearchTerm(productSalad.sku)
            .assertProductCard(productSalad)
            .assertProductsCount(1)
            // Search for variations sharing a part of SKU
            .enterSearchTerm(productCappuccino.sku + "-ALM")
            .assertProductCard(productCappuccinoAlmondMedium)
            .assertProductCard(productCappuccinoAlmondLarge)
            .assertProductsCount(2)
            // Search for exact variation SKU
            .enterSearchTerm(productCappuccinoAlmondLarge.sku)
            .assertProductCard(productCappuccinoAlmondLarge)
            .assertProductsCount(1)
            .leaveSearchMode()
    }
}
