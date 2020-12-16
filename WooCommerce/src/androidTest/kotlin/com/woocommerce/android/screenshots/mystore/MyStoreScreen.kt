package com.woocommerce.android.screenshots.mystore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.mystore.settings.SettingsScreen
import com.woocommerce.android.screenshots.util.Screen

class MyStoreScreen : Screen {
    companion object {
        const val MY_STORE = R.id.my_store_refresh_layout
        const val SETTINGS_BUTTON = R.id.menu_settings
    }

    val tabBar = TabNavComponent()
    val stats = StatsComponent()

    constructor(): super(MY_STORE)

    fun openSettingsPane(): SettingsScreen {
        onView(withId(SETTINGS_BUTTON)).perform(click())

        return SettingsScreen()
    }
}
