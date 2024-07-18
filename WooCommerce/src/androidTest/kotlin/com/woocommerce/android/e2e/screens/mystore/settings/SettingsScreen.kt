package com.woocommerce.android.e2e.screens.mystore.settings

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.NestedScrollViewExtension
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen
import org.junit.Assert

class SettingsScreen : Screen {
    // Using HELP_BUTTON even if we don't need to interact with it because for some reason Espresso can't find
    // LOG_OUT_BUTTON
    constructor() : super(R.id.option_help_and_support)

    fun openBetaFeatures(): BetaFeaturesScreen {
        clickOn(R.id.option_beta_features)
        return BetaFeaturesScreen()
    }

    fun goBackToMoreMenuScreen(): MoreMenuScreen {
        pressBack()
        return MoreMenuScreen()
    }

    fun setTheme(theme: String): SettingsScreen {
        clickOn(R.id.option_theme)

        val themeString = if (theme == "dark") {
            R.string.settings_app_theme_option_dark
        } else {
            R.string.settings_app_theme_option_light
        }

        val themeCheckbox: ViewInteraction = Espresso.onView(
            ViewMatchers.withText(getTranslatedString(themeString))
        )

        waitForElementToBeDisplayed(themeCheckbox)
        themeCheckbox.perform(ViewActions.click())

        return this
    }

    fun logOut() {
        if (!isElementCompletelyDisplayed(R.id.btn_option_logout)) {
            // We'd like to do this:
            //
            // scrollTo(LOG_OUT_BUTTON)
            //
            // But since the merge of the dark mode UI changes, it doesn't work anymore. Reading through the test
            // failure, it looks like it's failing because it can't find the LOG_OUT_BUTTON element. This is consistent
            // with the behavior that required the workaround to use SELECTED_STORE.
            //
            // Immediately attempting a scroll solves the issue.
            Espresso.onView(ViewMatchers.withId(R.id.btn_option_logout)).perform(NestedScrollViewExtension())
        }

        // retry in case log out pop up doesn't work the first time
        retryAction({
            // click log out menu item
            waitForElementToBeDisplayed(R.id.btn_option_logout)
            clickOn(R.id.btn_option_logout)

            // confirm log out action
            // sign out button is an Android system resources identifier
            waitForElementToBeDisplayed(android.R.id.button1)
            clickOn(android.R.id.button1)
        })

        // login screen should be displayed after logging out
        waitForElementToBeDisplayed((R.id.button_login_store))
    }

    private fun retryAction(action: () -> Unit, maxAttempts: Int = 3) {
        var attempts = 0
        var success = false
        var lastError: AssertionError? = null
        while (!success && attempts < maxAttempts) {
            try {
                action()
                success = true
            } catch (e: AssertionError) {
                lastError = e // Capture the last AssertionError
                Thread.sleep(1000) // Wait for 1 second before retrying
                attempts++
            }
        }
        if (!success) {
            Assert.fail("Failed to perform action after $maxAttempts attempts with error $lastError")
        }
    }
}
