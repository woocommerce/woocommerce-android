package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.mystore.DashboardScreen

class PasswordScreen : Screen {
    constructor() : super(org.wordpress.android.login.R.id.input)

    fun proceedWith(password: String): DashboardScreen {
        typeTextInto(org.wordpress.android.login.R.id.input, password)
        clickOn(R.id.bottom_button)

        return DashboardScreen()
    }
}
