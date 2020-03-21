package com.woocommerce.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
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
                .proceedWith("automatticwidgets.com")
                .proceedWith("nuttystephen.bem8kzmg@mailosaur.io")
                .proceedWithPassword()
                .proceedWith("*xBu(Z1Gr##Qo&SqaK0RBrqI")

                // Enable Products
                .openSettingsPane().openBetaFeatures()
                .enableProductEditing()
                .goBackToSettingsScreen().goBackToMyStoreScreen()

                // My Store
                .dismissTopBannerIfNeeded()
                .then<MyStoreScreen> { it.stats.switchToStatsDashboardYearsTab() }
                .thenTakeScreenshot<MyStoreScreen>("order-dashboard")

                // Orders
                .tabBar.gotoOrdersScreen()
                .thenTakeScreenshot<OrderListScreen>("order-list")
                .selectOrder(0)
                .thenTakeScreenshot<SingleOrderScreen>("order-detail")
                .goBackToOrdersScreen()

                .openSearchPane()
                .thenTakeScreenshot<OrderSearchScreen>("order-search")
                .cancel()

                // Reviews
                .tabBar.gotoReviewsScreen()
                .thenTakeScreenshot<ReviewsListScreen>("review-list")
                .selectReview(3)
                .thenTakeScreenshot<SingleReviewScreen>("review-details")
                .goBackToReviewsScreen()

                // Products
                .tabBar.gotoProductsScreen()
                .thenTakeScreenshot<ProductListScreen>("product-list")
                .selectProduct(0)
                .thenTakeScreenshot<SingleProductScreen>("product-details")
    }
}
