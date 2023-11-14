package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class CarouselScreen : Screen(SKIP_BUTTON) {
    companion object {
        val SKIP_BUTTON = R.id.button_skip
    }

    fun skip(): WelcomeScreen {
        clickOn(SKIP_BUTTON)
        return WelcomeScreen()
    }
}
