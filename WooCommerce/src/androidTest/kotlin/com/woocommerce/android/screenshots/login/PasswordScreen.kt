package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.Screen

class PasswordScreen : Screen {
    companion object {
        const val PASSWORD_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.bottom_button
    }

    constructor() : super(PASSWORD_FIELD)

    fun proceedWith(password: String): MyStoreScreen {
        typeTextInto(PASSWORD_FIELD, password)
        clickOn(NEXT_BUTTON)

        return MyStoreScreen()
    }
}
