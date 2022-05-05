package com.woocommerce.android.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.moremenu.MoreMenuScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.orders.OrderSearchScreen
import com.woocommerce.android.screenshots.orders.SingleOrderScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.products.SingleProductScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.reviews.SingleReviewScreen
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule

@HiltAndroidTest
class ScreenshotTest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @Before
    fun setUp() {
        CleanStatusBar.enableWithDefaults()
    }

    @After
    fun tearDown() {
        CleanStatusBar.disable()
    }

    @Test
    fun screenshots() {
        val testedTheme: String? = InstrumentationRegistry.getArguments().getString("theme")
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        if (testedTheme == "light" || testedTheme == "dark") {
            TabNavComponent()
                .gotoMoreMenuScreen()
                .openSettings(composeTestRule)
                .setTheme(testedTheme)
                .goBackToMoreMenuScreen()
            TabNavComponent().gotoMyStoreScreen()
        }

        // My Store
        MyStoreScreen()
            .stats.switchToStatsDashboardWeekTab()
            .thenTakeScreenshot<MyStoreScreen>("order-dashboard")

        // Orders
        TabNavComponent()
            .gotoOrdersScreen()
            .thenTakeScreenshot<OrderListScreen>("order-list")
            .selectOrder(7)
            .thenTakeScreenshot<SingleOrderScreen>("order-detail")
            .goBackToOrdersScreen()
            .openSearchPane()
            .thenTakeScreenshot<OrderSearchScreen>("order-search")
            .cancel()

        // More Menu
        TabNavComponent()
            .gotoMoreMenuScreen()
            .thenTakeScreenshot<MoreMenuScreen>("more-menu")

        // Reviews
        TabNavComponent()
            .gotoMoreMenuScreen()
            .openReviewsListScreen(composeTestRule)
            .thenTakeScreenshot<ReviewsListScreen>("review-list")
            .selectReviewByIndex(4)
            .thenTakeScreenshot<SingleReviewScreen>("review-details")
            .goBackToReviewsScreen()
            .goBackToMoreMenuScreen()

        // Products
        TabNavComponent()
            .gotoProductsScreen()
            .thenTakeScreenshot<ProductListScreen>("product-list")
            .selectProductByName("Akoya Pearl shades")
            .thenTakeScreenshot<SingleProductScreen>("product-details")
            .goBackToProductsScreen()
    }
}
