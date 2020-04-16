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
        const val TOOLBAR = R.id.toolbar
    }

    val tabBar = TabNavComponent()
    val stats = StatsComponent()

    constructor(): super(TOOLBAR)

    fun openSettingsPane(): SettingsScreen {
        openToolbarActionMenu()
        onView(withText(R.string.settings)).perform(click())

        return SettingsScreen()
    }

    fun dismissTopBannerIfNeeded(): MyStoreScreen {
        if (isElementDisplayed(R.id.dashboard_stats_availability_card)) {
            clickOn(R.id.my_store_availability_viewMore)
            clickOn(R.id.btn_no_thanks)
        }

        return this
    }
}
