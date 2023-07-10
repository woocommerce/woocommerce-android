@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.useMockedAPI
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.e2e.screens.orders.SingleOrderScreen
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OrdersRealAPI : TestBase() {
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
            .gotoOrdersScreen()
    }

    @After
    fun tearDown() {
        OrderListScreen()
            .leaveSearchMode()

        FilterScreen()
            .leaveFilterScreenToOrders()

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
    }

    private val order40 = OrderData(
        customerName = "Samuel Ayala",
        id = 40,
        productsTotalRaw = "10.00",
        taxesRaw = "0.00",
        shippingRaw = "0.00",
        totalRaw = "10.00",
        statusRaw = "pending",
        customerNoteRaw = "Cappuccino is made on doppio, free of charge. Enjoy!"
    )

    private val order41 = OrderData(
        customerName = "Samara Montes",
        id = 41,
        totalRaw = "7.00",
        statusRaw = "completed"
    )

    @Test
    @Ignore
    fun e2eRealApiOrdersFilter() {
        OrderListScreen()
            // Filter by "Order Status" = "Completed"
            .tapFilters()
            .filterByPropertyAndValue("Order Status", "Completed")
            .showOrders(true)
            .assertOrderCard(order41)
            .assertOrdersCount(1)
            // Check that "Clear" button works
            .tapFilters()
            .clearFilters()
            .showOrders(true)
            .assertOrderCard(order40)
            .assertOrderCard(order41)
            .assertOrdersCount(2)
    }

    @Test
    @Ignore
    fun e2eRealApiOrdersSearch() {
        OrderListScreen()
            // Make sure all orders are listed
            .assertOrderCard(order40)
            .assertOrderCard(order41)
            .assertOrdersCount(2)
            // Search by Customer Name (AKA Order Name)
            .openSearchPane()
            .enterSearchTerm(order40.customerName)
            .assertOrderCard(order40)
            .assertOrdersCount(1)
            .leaveSearchMode()
            // Search for non-existing order
            .openSearchPane()
            .enterAbsentSearchTerm("Absent Order")
            .assertSearchResultsAbsent("Absent Order")
            // Leave search and make sure all orders are listed
            .leaveSearchMode()
            .assertOrderCard(order40)
            .assertOrderCard(order41)
            .assertOrdersCount(2)
    }

    @Test
    @Ignore
    fun e2eRealApiOrderDetails() {
        try {
            OrderListScreen()
                .selectOrderById(order40.id)
                .assertOrderId(order40.id)
                .assertCustomerName(order40.customerName)
                .assertOrderStatus(order40.status)
                .assertOrderHasProduct(ProductsRealAPI().productSalad)
                .assertOrderHasProduct(ProductsRealAPI().productCappuccinoCocoMedium)
                .assertPayments(order40)
                .assertCustomerNote(order40.customerNote)
        } finally {
            SingleOrderScreen()
                .goBackToOrdersScreen()
        }
    }
}
