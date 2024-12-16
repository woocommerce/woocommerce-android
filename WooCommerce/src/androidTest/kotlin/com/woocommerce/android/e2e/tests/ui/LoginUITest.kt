@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.login.SiteAddressScreen
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginUITest: TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 4)
    var retryTestRule = RetryTestRule()

    @Before
    fun setUp() {
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
    }

    @Test
    fun siteAddressWithWrongURL() {
        SiteAddressScreen()
            .enterNonWPAddress(siteAddress = "notawpsite.com")
            .assertErrorElements()
    }
}
