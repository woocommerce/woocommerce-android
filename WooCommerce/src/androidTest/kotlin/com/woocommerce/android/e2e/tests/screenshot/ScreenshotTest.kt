package com.woocommerce.android.e2e.tests.screenshot

import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.mystore.DashboardScreen
import com.woocommerce.android.e2e.screens.notifications.NotificationsScreen
import com.woocommerce.android.e2e.screens.orders.CardReaderPaymentScreen
import com.woocommerce.android.e2e.screens.orders.UnifiedOrderScreen
import com.woocommerce.android.e2e.screens.products.ProductListScreen
import com.woocommerce.android.notifications.WooNotificationBuilder
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
import java.util.concurrent.TimeoutException
import javax.inject.Inject

@HiltAndroidTest
class ScreenshotTest : TestBase(failOnUnmatchedWireMockRequests = false) {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 3)
    val localeTestRule = LocaleTestRule()

    @get:Rule(order = 4)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject lateinit var wooNotificationBuilder: WooNotificationBuilder

    @Before
    fun setUp() {
        try {
            CleanStatusBar.enableWithDefaults()
        } catch (e: RuntimeException) {
            if (e.cause is TimeoutException) {
                Log.w("ScreenshotTest", e)
            } else {
                throw e
            }
        }
        rule.inject()
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
        DashboardScreen()
            .stats.switchToStatsDashboardMonthTab(composeTestRule)
            .thenTakeScreenshot<DashboardScreen>("order-dashboard")

        // Create Orders
        TabNavComponent()
            .gotoOrdersScreen()
            .createFABTap()
            .thenTakeScreenshot<UnifiedOrderScreen>("add-order")
            .goBackToOrdersScreen()

        // Capture In-Person Payment
        AppPrefs.setCardReaderWelcomeDialogShown() // Skip card reader welcome screen
        AppPrefs.setShowCardReaderConnectedTutorial(false) // Skip card reader tutorial
        TabNavComponent()
            .gotoOrdersScreen()
            .selectOrder(2)
            .tapOnCollectPayment()
            .chooseCardPayment()
            .thenTakeScreenshot<CardReaderPaymentScreen>("in-person-payments")
            .goBackToPaymentSelection()
            .goBackToOrderDetails()
            .goBackToOrdersScreen()

        // Create Products
        TabNavComponent()
            .gotoProductsScreen()
            .tapOnCreateProduct()
            .thenTakeScreenshot<ProductListScreen>("add-product")
            .goBackToProductList()

        NotificationsScreen(wooNotificationBuilder)
            .thenTakeScreenshot<NotificationsScreen>("push-notifications")
            .goBackToApp()
    }
}
