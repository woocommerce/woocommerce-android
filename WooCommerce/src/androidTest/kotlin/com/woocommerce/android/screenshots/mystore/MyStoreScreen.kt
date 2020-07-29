package com.woocommerce.android.screenshots.mystore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.mystore.settings.SettingsScreen
import com.woocommerce.android.screenshots.util.Screen

class MyStoreScreen : Screen {
    companion object {
        const val DASHBOARD = R.id.dashboard_refresh_layout

        const val STATS_CARD_DISMISS_BUTTON = R.id.btn_no_thanks

        const val SETTINGS_BUTTON_TEXT = R.string.settings
    }

    val tabBar = TabNavComponent()
    val stats = StatsComponent()

    constructor(): super(DASHBOARD)

    fun openSettingsPane(): SettingsScreen {
        openToolbarActionMenu()
        onView(withText(SETTINGS_BUTTON_TEXT)).perform(click())

        return SettingsScreen()
    }
}
