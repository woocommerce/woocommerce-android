@file:Suppress("DEPRECATION")

package com.woocommerce.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class WooPosExitPosDialogTest : TestBase() {
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
        dataStore.edit { it.clear() }
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent()
            .gotoMoreMenuScreen()
            .openPOSScreen(composeTestRule)
    }

    @Test
    fun testExitPOSDialogIsDisplayedWhenExitButtonClicked() = runTest {
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

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_menu_button"
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_popup_menu_${R.string.woopos_exit_confirmation_title}",
                useUnmergedTree = true
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog")
                .assertExists()
                .assertIsDisplayed()
            true
        }
    }

    @Test
    fun testExitPOSDialogIsDisplayedWithProperLabels() = runTest {
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

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_menu_button"
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_popup_menu_${R.string.woopos_exit_confirmation_title}",
                useUnmergedTree = true
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText(
                composeTestRule.activity.getString(R.string.woopos_exit_dialog_confirmation_title)
            ).assertIsDisplayed()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText(
                composeTestRule.activity.getString(R.string.woopos_exit_dialog_confirmation_message)
            ).assertIsDisplayed()
            true
        }
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText(
                composeTestRule.activity.getString(R.string.woopos_exit_dialog_confirmation_confirm_button)
            ).assertIsDisplayed()
            true
        }
    }

    @Test
    fun testExitPOSDialogIsDismissedWhenCloseButtonClicked() = runTest {
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

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_menu_button"
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_popup_menu_${R.string.woopos_exit_confirmation_title}",
                useUnmergedTree = true
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog_close").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog")
                .assertIsNotDisplayed()
            true
        }
    }

    @Test
    fun testExitPOSDialogIsDismissedWhenExitButtonClicked() = runTest {
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

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_menu_button"
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_popup_menu_${R.string.woopos_exit_confirmation_title}",
                useUnmergedTree = true
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog_exit").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog")
                .assertIsNotDisplayed()
            true
        }
    }

    @Test
    fun testWoPosHomeScreenIsDismissedWhenExitButtonClicked() = runTest {
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

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_menu_button"
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag(
                "woo_pos_popup_menu_${R.string.woopos_exit_confirmation_title}",
                useUnmergedTree = true
            ).performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_exit_confirmation_dialog_exit").performClick()
            true
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("woo_pos_home_screen")
                .assertIsNotDisplayed()
            true
        }
    }
}
