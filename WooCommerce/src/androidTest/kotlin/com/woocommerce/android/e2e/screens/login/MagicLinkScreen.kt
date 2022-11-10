package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class MagicLinkScreen : Screen {
    companion object {
        const val USE_PASSWORD_BUTTON = R.id.login_enter_password
    }

    constructor() : super(USE_PASSWORD_BUTTON)

    fun proceedWithPassword(): PasswordScreen {
        clickOn(USE_PASSWORD_BUTTON)
        return PasswordScreen()
    }
}
