package com.woocommerce.android.ui.main

import androidx.test.espresso.Espresso
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.products.ProductListScreen
import com.woocommerce.android.screenshots.util.ProductData
import com.woocommerce.android.screenshots.util.MocksReader
import com.woocommerce.android.screenshots.util.iterator
import com.woocommerce.android.ui.prefs.cardreader.hub.CardReaderHubViewHolder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class IPPAccessibilityTest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)

        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)
    }

    @Test
    fun ippOptionOnSettingsClickedAccessibilityCheck() {
        Espresso.onView(ViewMatchers.withId(R.id.menu_settings))
            .perform(click())
        try {
            Thread.sleep(500.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }

    @Test
    fun ippManageCardReaderOptionOnSettingsClickedAccessibilityCheck() {
        Espresso.onView(ViewMatchers.withId(R.id.menu_settings))
            .perform(click())
        idleFor(500)
        Espresso.onView(ViewMatchers.withId(R.id.option_card_reader_payments))
            .perform(click())
        idleFor(500)
        Espresso.onView(ViewMatchers.withId(R.id.cardReaderHubRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<CardReaderHubViewHolder>(1, click()))
    }

    private fun idleFor(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }
}
