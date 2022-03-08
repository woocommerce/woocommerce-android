package com.woocommerce.android.screenshots.mystore.settings

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.moremenu.MoreMenuScreen
import com.woocommerce.android.screenshots.util.NestedScrollViewExtension
import com.woocommerce.android.screenshots.util.Screen

class SettingsScreen : Screen {
    companion object {
        const val HELP_BUTTON = R.id.option_help_and_support
        const val BETA_FEATURES_BUTTON = R.id.option_beta_features
        const val LOG_OUT_BUTTON = R.id.btn_option_logout
    }

    // Using HELP_BUTTON even if we don't need to interact with it because for some reason Espresso can't find
    // LOG_OUT_BUTTON
    constructor() : super(HELP_BUTTON)

    fun openBetaFeatures(): BetaFeaturesScreen {
        clickOn(BETA_FEATURES_BUTTON)
        return BetaFeaturesScreen()
    }

    fun goBackToMoreMenuScreen(): MoreMenuScreen {
        pressBack()
        return MoreMenuScreen()
    }

    fun setTheme(theme: String): SettingsScreen {
        clickOn(R.id.option_theme)

        val themeString = if (theme == "dark")
            R.string.settings_app_theme_option_dark
        else
            R.string.settings_app_theme_option_light

        val themeCheckbox: ViewInteraction = Espresso.onView(
            ViewMatchers.withText(getTranslatedString(themeString))
        )

        waitForElementToBeDisplayed(themeCheckbox)
        themeCheckbox.perform(ViewActions.click())

        return this
    }

    fun logOut() {
        if (!isElementCompletelyDisplayed(LOG_OUT_BUTTON)) {
            // We'd like to do this:
            //
            // scrollTo(LOG_OUT_BUTTON)
            //
            // But since the merge of the dark mode UI changes, it doesn't work anymore. Reading through the test
            // failure, it looks like it's failing because it can't find the LOG_OUT_BUTTON element. This is consistent
            // with the behavior that required the workaround to use SELECTED_STORE.
            //
            // Immediately attempting a scroll solves the issue.
            Espresso.onView(ViewMatchers.withId(LOG_OUT_BUTTON)).perform(NestedScrollViewExtension())
        }

        clickOn(LOG_OUT_BUTTON)

        // Confirm Log Out
        clickButtonInDialogWithTitle(R.string.signout)
    }
}
