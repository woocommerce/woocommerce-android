package com.woocommerce.android.e2e.screens.login

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.TabNavComponent

class WelcomeScreen : Screen {
    companion object {
        fun logoutIfNeeded(composeTestRule: ComposeContentTestRule): WelcomeScreen {
            if (isElementDisplayed(R.id.my_store)) {
                TabNavComponent()
                    .gotoMoreMenuScreen()
                    .openSettings(composeTestRule)
                    .logOut()
            }

            Thread.sleep(1000)

            return skipCarouselIfNeeded()
        }

        fun skipCarouselIfNeeded(): WelcomeScreen {
            return if (Screen.isElementDisplayed(CarouselScreen.SKIP_BUTTON)) {
                CarouselScreen().skip()
            } else {
                WelcomeScreen()
            }
        }
    }

    constructor() : super(R.id.button_login_store)

    fun selectLogin(): SiteAddressScreen {
        clickOn(R.id.button_login_store)
        return SiteAddressScreen()
    }

    fun selectWPCOMLogin(): EmailAddressScreen {
        clickOn(R.id.button_login_wpcom)
        return EmailAddressScreen()
    }
}
