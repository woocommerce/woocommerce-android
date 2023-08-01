package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class MagicLinkScreen : Screen {
    constructor() : super(R.id.login_enter_password)

    fun proceedWithPassword(): PasswordScreen {
        clickOn(R.id.login_enter_password)
        return PasswordScreen()
    }
}
