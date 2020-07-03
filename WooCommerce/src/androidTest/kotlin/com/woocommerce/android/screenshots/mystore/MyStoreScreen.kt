package com.woocommerce.android.screenshots.mystore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.settings.SettingsScreen
import com.woocommerce.android.screenshots.util.Screen

class MyStoreScreen : Screen(DASHBOARD) {
    companion object {
        const val DASHBOARD = R.id.dashboard_refresh_layout
        const val STATS_CARD = R.id.dashboard_stats_availability_card
        const val STATS_CARD_VIEW_MORE = R.id.dashboard_stats_availability_card
        const val STATS_CARD_DISMISS_BUTTON = R.id.btn_no_thanks
        const val SETTINGS_BUTTON_TEXT = R.string.settings
    }

    val tabBar = TabNavComponent()
    val stats = StatsComponent()

    fun openSettingsPane(): SettingsScreen {
        actionOps.openToolbarActionMenu()
        onView(withText(SETTINGS_BUTTON_TEXT)).perform(click())
        return SettingsScreen()
    }

    fun dismissTopBannerIfNeeded(): MyStoreScreen {
        if (isElementDisplayed(STATS_CARD)) {
            clickOps.clickOn(STATS_CARD_VIEW_MORE)
            clickOps.clickOn(STATS_CARD_DISMISS_BUTTON)
        }
        return this
    }

    fun logOut(): WelcomeScreen {
        openSettingsPane().logOut()
        return WelcomeScreen()
    }
}
