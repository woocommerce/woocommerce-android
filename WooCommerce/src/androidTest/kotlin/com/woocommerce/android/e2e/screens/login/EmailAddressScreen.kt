package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class EmailAddressScreen : Screen {
    companion object {
        const val EMAIL_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.login_continue_button
    }

    constructor() : super(EMAIL_ADDRESS_FIELD)

    fun proceedWith(emailAddress: String): PasswordScreen {
        typeTextInto(EMAIL_ADDRESS_FIELD, emailAddress)
        clickOn(NEXT_BUTTON)

        return PasswordScreen()
    }
}
