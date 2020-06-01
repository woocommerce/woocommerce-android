package com.woocommerce.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
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

//    @Test
//    fun screenshots() {
//        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
//
//        var SCREENSHOTS_URL = "https://usual-frigate.jurassic.ninja/"
//        var SCREENSHOTS_USERNAME = "tapan.shah21@gmail.com"
//        var SCREENSHOTS_PASSWORD = "Appletree0!"
//
//        WelcomeScreen
//            .logoutIfNeeded()
//            .selectLogin()
//            // Connect a WooCommerce store by URL
//            .proceedWith(SCREENSHOTS_URL)
//            // Enter email address
//            .proceedWith(SCREENSHOTS_USERNAME)
//            // No magic link, this is an automated test, enter password manually
//            //.proceedWithPassword()
//            .proceedWith(SCREENSHOTS_PASSWORD)
//
//        // My Store
//        // When debugging these tests, you might want to save time and avoid the logout - login flow above.
//        MyStoreScreen()
//            .dismissTopBannerIfNeeded()
//            .then<MyStoreScreen> { it.stats.switchToStatsDashboardYearsTab() }
//    }
}
