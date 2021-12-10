package com.woocommerce.android.ui.main

import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.login.WelcomeScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginScreenAccessibilityTest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
    }

    @Test
    fun welcomeScreenAccessibilityCheck() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
    }

    @Test
    fun storeAddressScreenAccessibilityCheck() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
    }

    @Test
    fun emailAddressScreenAccessibilityCheck() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
    }

    @Test
    fun passwordScreenAccessibilityCheck() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)
    }
}
