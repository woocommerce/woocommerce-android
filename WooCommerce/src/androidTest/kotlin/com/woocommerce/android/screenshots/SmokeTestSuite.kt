package com.woocommerce.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.ui.main.MainActivity
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.DEFAULT)
class SmokeTestSuite {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    @Test
    fun loginUsingEmailSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            // Connect a WooCommerce store by URL
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            // Enter email address
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            // No magic link, this is an automated test, enter password manually
            .proceedWithPassword()
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        // My Store
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
        MyStoreScreen()
            .dismissTopBannerIfNeeded()
            .then<MyStoreScreen> { it.isLoggedIn() }
    }

    @Test
    fun loginUsingSiteCredentialsSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            // Connect a WooCommerce store by URL
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            // Use site credentials instead of email address
            .proceedWithSiteLogin()
            // No magic link, this is an automated test, enter username/password manually
            .proceedWith(BuildConfig.SCREENSHOTS_SITE_USERNAME, BuildConfig.SCREENSHOTS_SITE_PASSWORD)
            // enter your password instead
            .proceedWithPassword()
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        // My Store
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
        MyStoreScreen()
            .then<MyStoreScreen> { it.isLoggedIn() }
    }

    @Test
    fun searchOrdersSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        OrderListScreen
            .navigateToOrders()
            .searchOrdersByName()
            .selectRandomOrderFromTheSearchResult()
            .scrollToOrderDetails()
            // Close Order details and go back to search
            .goBackToSearch()
            // Go back to Orders view
            .cancelSearch()

        // Orders
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
    }

    @Test
    fun updateOrderInfoSuccess() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        OrderListScreen
            .navigateToOrders()
            .selectRandomOrderFromTheList()
            .scrollToOrderDetails()
            // add product notes and email update to customer
            .emailOrderNoteToCustomer()
            // Close Order details and go back to orders list
            .goBackToOrderList()

        // Orders
        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
        OrderListScreen()
            .then<OrderListScreen> { it.isTitle("Orders") }
    }
}
