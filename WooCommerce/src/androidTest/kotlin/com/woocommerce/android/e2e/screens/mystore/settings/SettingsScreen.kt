package com.woocommerce.android.e2e.screens.mystore.settings

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.moremenu.MoreMenuScreen

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
        var attempts = 0
        while (!isElementCompletelyDisplayed(R.id.btn_option_logout) && attempts < 2) {
            scrollTo(R.id.btn_option_logout)
            attempts++
        }

        waitForElementToBeDisplayed(R.id.btn_option_logout)
        clickOn(R.id.btn_option_logout)

        // Confirm Log Out
        waitForElementToBeDisplayed(android.R.id.button1) // sign out button is an Android system resources identifier
        clickButtonInDialogWithTitle(R.string.signout)
    }
}
