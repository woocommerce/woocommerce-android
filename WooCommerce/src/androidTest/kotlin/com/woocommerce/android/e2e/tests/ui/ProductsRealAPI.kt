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
import com.woocommerce.android.e2e.screens.shared.FilterScreen
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

        FilterScreen()
            .leaveFilterScreenToProducts()

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
    }

    val productSalad = ProductData(
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
        name = productCappuccino.name,
        stockStatusRaw = productCappuccino.stockStatusRaw,
        priceDiscountedRaw = "3",
        sku = productCappuccino.sku + "-ALM-M"
    )

    private val productCappuccinoAlmondLarge = ProductData(
        name = productCappuccino.name,
        stockStatusRaw = productCappuccino.stockStatusRaw,
        priceDiscountedRaw = "4",
        sku = productCappuccino.sku + "-ALM-L"
    )

    val productCappuccinoCocoMedium = ProductData(
        name = productCappuccino.name,
        stockStatusRaw = productCappuccino.stockStatusRaw,
        priceDiscountedRaw = "3",
        sku = productCappuccino.sku + "-COCO-M"
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
            .leaveSearchMode()
            // Search for 'productSalad'
            .openSearchPane()
            .enterSearchTerm(productSalad.name)
            .assertProductCard(productSalad)
            .assertProductsCount(1)
            .leaveSearchMode()
            // Search for non-existing product
            .openSearchPane()
            .enterAbsentSearchTerm("Unexisting Product")
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
            // Search for a simple product SKU
            .openSearchPane()
            .tapSearchSKU()
            .enterSearchTerm(productSalad.sku)
            .assertProductCard(productSalad)
            .assertProductsCount(1)
            .leaveSearchMode()
            // Search for variations sharing a part of SKU
            .openSearchPane()
            .tapSearchSKU()
            .enterSearchTerm(productCappuccino.sku + "-ALM")
            .assertProductCard(productCappuccinoAlmondMedium)
            .assertProductCard(productCappuccinoAlmondLarge)
            .assertProductsCount(2)
            .leaveSearchMode()
            // Search for exact variation SKU
            .openSearchPane()
            .tapSearchSKU()
            .enterSearchTerm(productCappuccinoAlmondLarge.sku)
            .assertProductCard(productCappuccinoAlmondLarge)
            .assertProductsCount(1)
            .leaveSearchMode()
    }

    @Test
    fun e2eRealApiProductsFilter() {
        ProductListScreen()
            // Filter by "Product type" = "Simple"
            .tapFilters()
            .filterByPropertyAndValue("Product type", "Simple")
            .showProducts(true)
            .assertProductCard(productSalad)
            .assertProductsCount(1)
            // Check that "Clear" button works
            .tapFilters()
            .clearFilters()
            .showProducts(true)
            .assertProductCard(productSalad)
            .assertProductCard(productCappuccino)
            .assertProductsCount(2)
            // Filter by "Stock status" = "Out of Stock" and expect to see zero products
            .tapFilters()
            .filterByPropertyAndValue("Stock status", "Out of stock")
            .showProducts(false)
            .assertProductsCount(0)
    }

    @Test
    fun e2eRealApiProductsSort() {
        ProductListScreen()
            // Check the default sort (A-Z)
            .assertProductIsAtPosition(productCappuccino.name, 0)
            .assertProductIsAtPosition(productSalad.name, 1)
            // Sort Z to A
            .tapSort()
            .selectSortOption("Title: Z to A")
            .assertProductIsAtPosition(productCappuccino.name, 1)
            .assertProductIsAtPosition(productSalad.name, 0)
    }
}
