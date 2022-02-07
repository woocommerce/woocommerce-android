package com.woocommerce.android.screenshots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.orders.OrderSearchScreen
import com.woocommerce.android.screenshots.orders.SingleOrderScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.products.SingleProductScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.reviews.SingleReviewScreen
import com.woocommerce.android.screenshots.util.Screen
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.*
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
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        if (testedTheme == "light" || testedTheme == "dark") {
            MyStoreScreen().openSettingsPane().setTheme(testedTheme).goBackToMyStoreScreen()
        }

        // My Store
        MyStoreScreen()
            .thenTakeScreenshot<MyStoreScreen>("order-dashboard")

        // Orders
        TabNavComponent()
            .gotoOrdersScreen()
            .thenTakeScreenshot<OrderListScreen>("order-list")
            .selectOrder(0)
            .thenTakeScreenshot<SingleOrderScreen>("order-detail")
            .goBackToOrdersScreen()
            .openSearchPane()
            .thenTakeScreenshot<OrderSearchScreen>("order-search")
            .cancel()

        // Reviews
        TabNavComponent()
            .gotoReviewsScreen()
            .thenTakeScreenshot<ReviewsListScreen>("review-list")
            .selectReviewByTitle("Mira Workman left a review on Colorado shades")
            .thenTakeScreenshot<SingleReviewScreen>("review-details")
            .goBackToReviewsScreen()

        // Products
        TabNavComponent()
            .gotoProductsScreen()
            .thenTakeScreenshot<ProductListScreen>("product-list")
            .selectProductByName("Akoya Pearl shades")
            .thenTakeScreenshot<SingleProductScreen>("product-details")
            .goBackToProductsScreen()
    }
}
