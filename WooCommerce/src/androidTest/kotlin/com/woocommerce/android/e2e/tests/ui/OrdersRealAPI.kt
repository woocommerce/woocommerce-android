@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.useMockedAPI
import com.woocommerce.android.e2e.rules.Retry
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.e2e.screens.orders.SingleOrderScreen
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

    @get:Rule(order = 4)
    var retryTestRule = RetryTestRule()

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
            .leaveOrClearSearchMode()

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
    }

    @Retry(numberOfTimes = 1)
    @Test
    fun e2eRealApiOrdersFilter() {
        OrderListScreen()
            // Filter by "Order Status" = "Completed"
            .tapFilters()
            .filterByPropertyAndValue("Order Status", "Completed")
            .showOrders(true)
            .assertOrderCard(completedOrder)
            .assertOrdersCount(1)
            // Check that "Clear" button works
            .tapFilters()
            .clearFilters()
            .showOrders(true)
            .assertOrderCard(pendingOrder)
            .assertOrderCard(completedOrder)
            .assertOrdersCount(2)
    }

    @Retry(numberOfTimes = 1)
    @Test
    fun e2eRealApiOrdersSearch() {
        OrderListScreen()
            // Make sure all orders are listed
            .assertOrderCard(pendingOrder)
            .assertOrderCard(completedOrder)
            .assertOrdersCount(2)
            // Search by Customer Name (AKA Order Name)
            .openSearchPane()
            .enterSearchTerm(pendingOrder.customerName)
            .assertOrderCard(pendingOrder)
            .assertOrdersCount(1)
            .leaveOrClearSearchMode()
            // Search for non-existing order
            .openSearchPane()
            .enterAbsentSearchTerm("Absent Order")
            .assertSearchResultsAbsent("Absent Order")
            // Leave search and make sure all orders are listed
            .leaveOrClearSearchMode()
            .assertOrderCard(pendingOrder)
            .assertOrderCard(completedOrder)
            .assertOrdersCount(2)
    }

    @Retry(numberOfTimes = 1)
    @Test
    @Ignore
    fun e2eRealApiOrderDetails() {
        try {
            OrderListScreen()
                .selectOrderById(pendingOrder.id)
                .assertOrderId(pendingOrder.id)
                .assertCustomerName(pendingOrder.customerName)
                .assertOrderStatus(pendingOrder.status)
                .assertOrderHasProduct(productSalad)
                .assertOrderHasProduct(productCappuccinoCocoMedium)
                .assertPayments(pendingOrder)
                .assertCustomerNote(pendingOrder.customerNote)
        } finally {
            SingleOrderScreen()
                .goBackToOrdersScreen()
        }
    }
}
