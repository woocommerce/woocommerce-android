package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class MagicLinkScreen : Screen {
    constructor() : super(R.id.login_magic_link_fallback_button)

    fun proceedWithPassword(): PasswordScreen {
        clickOn(R.id.login_magic_link_fallback_button)
        return PasswordScreen()
    }
}
