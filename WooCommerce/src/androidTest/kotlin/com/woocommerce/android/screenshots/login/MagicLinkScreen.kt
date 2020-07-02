package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class MagicLinkScreen : Screen(USE_PASSWORD_BUTTON) {
    companion object {
        const val USE_PASSWORD_BUTTON = R.id.login_enter_password
    }

    fun proceedWithPassword(): PasswordScreen {
        clickOn(USE_PASSWORD_BUTTON)
        return PasswordScreen()
    }
}
