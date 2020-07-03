package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.Screen

class WelcomeScreen : Screen(LOGIN_BUTTON) {
    companion object {
        const val LOGIN_BUTTON = R.id.button_login
        private const val DASHBOARD = R.id.dashboard

        fun logoutIfNeeded(): WelcomeScreen {
            if (isElementDisplayed(DASHBOARD)) {
                MyStoreScreen().openSettingsPane().logOut()
            }

            return WelcomeScreen()
        }
    }

    fun selectLogin(): SiteAddressScreen {
        clickOps.clickOn(LOGIN_BUTTON)
        return SiteAddressScreen()
    }
}
