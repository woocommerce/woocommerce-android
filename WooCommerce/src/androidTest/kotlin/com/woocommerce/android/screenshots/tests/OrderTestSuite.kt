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

        // Orders
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
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
            // add product notes and email update to customer
            .emailOrderNoteToCustomer()
            // Close Order details and go back to orders list
            .goBackToOrderList()

        // Orders
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
            .logOut()
    }
}

