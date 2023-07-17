package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class EmailAddressScreen : Screen {
    constructor() : super(org.wordpress.android.login.R.id.input)

    fun proceedWith(emailAddress: String): PasswordScreen {
        typeTextInto(org.wordpress.android.login.R.id.input, emailAddress)
        clickOn(R.id.login_continue_button)

        return PasswordScreen()
    }
}
