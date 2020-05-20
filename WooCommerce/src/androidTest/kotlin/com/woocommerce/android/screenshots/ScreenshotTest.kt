package com.woocommerce.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.orders.SingleOrderScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun screenshots() {
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
            // Let's start by taking the screenshots for the dark mode. The first step, making sure that we're actually
            // in dark mode.
            .openSettingsPane()
            .switchToDarkTheme()
            .then<MyStoreScreen> { it.stats.switchToStatsDashboardYearsTab() }
            .thenTakeScreenshot<MyStoreScreen>("order-dashboard")

            // Order
            .tabBar.gotoOrdersScreen()
            .selectOrder(0)
            // Give time to the images to load
            .then<SingleOrderScreen> { it.idleFor(1000) }
            .thenTakeScreenshot<SingleOrderScreen>("order-detail")
            .goBackToOrdersScreen()

            // Reviews
            .tabBar.gotoReviewsScreen()
            .thenTakeScreenshot<ReviewsListScreen>("review-list")

            // Now, let's switch light mode and take the remaining screenshots
            .tabBar.gotoMyStoreScreen()
            .openSettingsPane()
            .switchToLightTheme()

            // Orders
            .tabBar.gotoOrdersScreen()
            .thenTakeScreenshot<OrderListScreen>("order-list")

            // Products
            .tabBar.gotoProductsScreen()
            .thenTakeScreenshot<ProductListScreen>("product-list")
    }
}
