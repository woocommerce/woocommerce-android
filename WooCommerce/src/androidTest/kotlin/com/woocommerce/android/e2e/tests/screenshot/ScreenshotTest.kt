package com.woocommerce.android.e2e.tests.screenshot

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
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
    var locationPermissionRule: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule(order = 5)
    var bluetoothConnectPermissionRule: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_CONNECT)

    @get:Rule(order = 6)
    var bluetoothScanPermissionRule: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_SCAN)

    @get:Rule(order = 7)
    var notificationsPermissionRule: GrantPermissionRule? = GrantPermissionRule
        .grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 8)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var wooNotificationBuilder: WooNotificationBuilder

    @Before
    fun setUp() {
        cleanStatusBar()
        rule.inject()
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
            .selectOrderById(2787)
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
            .tapOnAddManually(composeTestRule)
            .thenTakeScreenshot<ProductListScreen>("add-product")
            .goBackToProductList()

        NotificationsScreen(wooNotificationBuilder)
            .thenTakeScreenshot<NotificationsScreen>("push-notifications")
            .goBackToApp()
    }

    private fun cleanStatusBar() {
        fun getSystemUiDemoIntent() = Intent("com.android.systemui.demo").setPackage("com.android.systemui")
        getSystemUiDemoIntent()
            .putExtra("command", "clock")
            .putExtra("hhmm", "1230").apply {
                appContext.sendOrderedBroadcast(this, null)
            }
        getSystemUiDemoIntent()
            .putExtra("command", "network")
            .putExtra("mobile", "hide").apply {
                appContext.sendOrderedBroadcast(this, null)
            }
    }
}
