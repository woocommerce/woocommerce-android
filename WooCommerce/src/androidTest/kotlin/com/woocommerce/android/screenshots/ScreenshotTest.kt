package com.woocommerce.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.orders.OrderSearchScreen
import com.woocommerce.android.screenshots.orders.SingleOrderScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.products.SingleProductScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.reviews.SingleReviewScreen
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
//        activityRule.launchActivity(null)
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen.logoutIfNeeded()
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
                // To do so, comment the whole block above and then uncomment the line below. Note that this implies the
                // app in the Emulator is already logged in.
                //MyStoreScreen()
                .dismissTopBannerIfNeeded()
                .then<MyStoreScreen> { it.stats.switchToStatsDashboardYearsTab() }
                .thenTakeScreenshot<MyStoreScreen>("order-dashboard")

                // Orders
                .tabBar.gotoOrdersScreen()
                .thenTakeScreenshot<OrderListScreen>("order-list")
                .selectOrder(0)
                .thenTakeScreenshot<SingleOrderScreen>("order-detail")
                .goBackToOrdersScreen()

                // Reviews
                .tabBar.gotoReviewsScreen()
                .thenTakeScreenshot<ReviewsListScreen>("review-list")

                // Products
                .tabBar.gotoProductsScreen()
                .thenTakeScreenshot<ProductListScreen>("product-list")
    }
}
