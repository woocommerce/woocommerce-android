package com.woocommerce.android.screenshots

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

@HiltAndroidTest
class MocksTest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    val localeTestRule = LocaleTestRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mocksDemo() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            // Connect a WooCommerce store by URL
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        Thread.sleep(1000000)
    }
}
