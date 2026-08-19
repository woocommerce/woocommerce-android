@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.screenshot

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.woopos.WooPosHomeScreen
import com.woocommerce.android.e2e.screens.woopos.WooPosPaymentSuccessScreen
import com.woocommerce.android.e2e.screens.woopos.WooPosTotalsScreen
import com.woocommerce.android.e2e.tests.FAKE_PASSWORD
import com.woocommerce.android.e2e.tests.FAKE_URL
import com.woocommerce.android.e2e.tests.FAKE_USERNAME
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@HiltAndroidTest
class WooPosScreenshotTest : TestBase(failOnUnmatchedWireMockRequests = false) {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 3)
    val localeTestRule = LocaleTestRule()

    @get:Rule(order = 4)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 5)
    var retryTestRule = RetryTestRule()

    @Before
    fun setUp() {
        cleanStatusBar()
        lockLandscapeOrientation()
        rule.inject()
    }

    private fun lockLandscapeOrientation() {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put system user_rotation 0")
            .close()
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put system accelerometer_rotation 0")
            .close()
    }

    @Test
    fun posScreenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(FAKE_URL)
            .proceedWith(FAKE_USERNAME)
            .proceedWith(FAKE_PASSWORD)

        val homeScreen = TabNavComponent()
            .gotoPosScreen()
            .waitForLoad(composeTestRule)
            .addProductsToCart(composeTestRule)
            .thenTakeScreenshot<WooPosHomeScreen>("pos-home")

        val totalsScreen = homeScreen.proceedToCheckout(composeTestRule)
            .waitForLoad(composeTestRule)
            .thenTakeScreenshot<WooPosTotalsScreen>("pos-totals")

        val cashPaymentScreen = totalsScreen.selectCashPayment(composeTestRule)

        cashPaymentScreen.completePayment(composeTestRule)
            .waitForScreen(composeTestRule)
            .thenTakeScreenshot<WooPosPaymentSuccessScreen>("pos-payment-success")
    }

    private fun cleanStatusBar() {
        fun getSystemUiDemoIntent() =
            android.content.Intent("com.android.systemui.demo").setPackage("com.android.systemui")
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
