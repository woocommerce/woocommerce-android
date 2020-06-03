package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class MagicLinkScreen : Screen {
    companion object {
        const val USE_PASSWORD_BUTTON = R.id.login_enter_password
        const val LOGIN_WITH_SITE_CREDENTIALS_BUTTON = R.id.login_site_button
    }

    constructor(): super(USE_PASSWORD_BUTTON)

    fun proceedWithPassword(): PasswordScreen {
        clickOn(USE_PASSWORD_BUTTON)
        return PasswordScreen()
    }
}
