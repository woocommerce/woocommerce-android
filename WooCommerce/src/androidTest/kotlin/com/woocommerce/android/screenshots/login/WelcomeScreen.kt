package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen.Companion
import com.woocommerce.android.screenshots.util.Screen

class WelcomeScreen : Screen {
    companion object {
        const val LOGIN_BUTTON = R.id.button_login

        fun logoutIfNeeded(): WelcomeScreen {
            if (isElementDisplayed(R.id.dashboard)) {
                MyStoreScreen().openSettingsPane().logOut()
            }

            Thread.sleep(1000)

            return WelcomeScreen()
        }
    }

    constructor() : super(LOGIN_BUTTON)

    fun selectLogin(): SiteAddressScreen {
        clickOn(LOGIN_BUTTON)
        return SiteAddressScreen()
    }
}
