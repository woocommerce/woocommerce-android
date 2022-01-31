package com.woocommerce.android.screenshots

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.*
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
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

    @Test
    fun screenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        // My Store
        TabNavComponent()
            .gotoReviewsScreen()
            .thenTakeScreenshot<ReviewsListScreen>("reviews-list")
    }
}
