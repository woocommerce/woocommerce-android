@file:Suppress("DEPRECATION")

package com.woocommerce.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooPosBannerTest : TestBase() {
    @get:Rule(order = 1)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 3)
    val initRule = InitializationRule()

    @get:Rule(order = 4)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 5)
    var retryTestRule = RetryTestRule()

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() = runTest {
        rule.inject()
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent()
            .gotoMoreMenuScreen()
            .openPOSScreen(composeTestRule)
        dataStore.edit { it.clear() }
    }

    @Test
    fun testWooPosSimpleProductsOnlyBannerIsDisplayedOnProductsScreen()  = runTest {

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag(
            "woo_pos_simple_products_banner"
        ).assertIsDisplayed()
    }

    @Test
    fun testWooPosSimpleProductsOnlyBannerTitleIsDisplayedOnBanner()  = runTest {

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.woopos_banner_simple_products_only_title)
        ).assertIsDisplayed()
    }

    @Test
    fun testWooPosSimpleProductsOnlyBannerMessageIsDisplayedOnBanner()  = runTest {

        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("product_list")
                    .assertExists()
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.woopos_banner_simple_products_only_message)
                + " "
                + composeTestRule.activity.getString(R.string.woopos_banner_simple_products_only_message_learn_more)
        ).assertIsDisplayed()
    }

}
