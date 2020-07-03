package com.woocommerce.android.screenshots.tests

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.ui.main.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import tools.fastlane.screengrab.locale.LocaleTestRule

open class TestBase {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            // Connect a WooCommerce store by URL
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            // Enter email address
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            // No magic link, this is an automated test, enter password manually
            .proceedWithPassword()
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)
    }

    @After
    open fun tearDown() {
    }
}
