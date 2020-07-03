package com.woocommerce.android.screenshots.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.screenshots.orders.OrderListScreen
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OrderTestSuite : TestBase() {
    @Test
    fun searchOrdersSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        OrderListScreen
            .navigateToOrders()
            .searchOrdersByName()
            .selectRandomOrderFromTheSearchResult()
            .scrollToNotesDetails()
            // Close Order details and go back to search
            .goBackToSearch()
            // Go back to Orders view
            .cancelSearch()

        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
            .logOut()
    }

    @Test
    fun updateOrderInfoSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        OrderListScreen
            .navigateToOrders()
            .selectRandomOrderFromTheList()
            .checkBillingInfo()
            .scrollToNotesDetails()
            // add order notes
            .addOrderNote()
            // Close Order details and go back to orders list
            .goBackToOrderList()

        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
            .logOut()
    }

    @Test
    fun issueRefundSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        OrderListScreen
            .navigateToOrders()
            .selectRandomOrderFromTheList()
            .scrollToPaymentDetails()
            // select product item, quantity, and add reason for refund
            .issueRefund()
            // Close Order details and go back to orders list
            .goBackToOrderList()

        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
            .logOut()
    }
}

