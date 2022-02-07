package com.woocommerce.android.screenshots.mystore.settings

import android.view.KeyEvent
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.NestedScrollViewExtension
import com.woocommerce.android.screenshots.util.Screen


class SettingsScreen : Screen {
    companion object {
        const val SELECTED_STORE = R.id.option_store
        const val BETA_FEATURES_BUTTON = R.id.option_beta_features
        const val LOG_OUT_BUTTON = R.id.btn_option_logout
    }

    // Using SELECTED_STORE even if we don't need to interact with it because for some reason Espresso can't find
    // LOG_OUT_BUTTON
    constructor() : super(SELECTED_STORE)

    fun openBetaFeatures(): BetaFeaturesScreen {
        clickOn(BETA_FEATURES_BUTTON)
        return BetaFeaturesScreen()
    }

    fun goBackToMyStoreScreen(): MyStoreScreen {
        pressBack()
        return MyStoreScreen()
    }

    fun setTheme(theme: String): SettingsScreen {
        clickOn(R.id.option_theme)

        val themeSelector: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.select_dialog_listview))

        waitForElementToBeDisplayed(themeSelector)

        // The theme radio buttons have no IDs, and we can't rely on text
        // because we need to test under different locales.
        // This solution relies on buttons order and
        // will move focus to the top of the list first ("Light"):
        themeSelector
            .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_UP))
            .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_UP))
            .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_UP))

        // Move focus down if we need to set "Dark":
        if (theme == "dark") {
            themeSelector.perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
        }

        themeSelector.perform(ViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
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
