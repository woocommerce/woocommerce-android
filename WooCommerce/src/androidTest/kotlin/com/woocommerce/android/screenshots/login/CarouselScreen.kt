package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class CarouselScreen : Screen(SKIP_BUTTON) {
    companion object {
        val SKIP_BUTTON = R.id.button_skip
    }

    fun skip(): WelcomeScreen {
        clickOn(SKIP_BUTTON)
        return WelcomeScreen()
    }
}
