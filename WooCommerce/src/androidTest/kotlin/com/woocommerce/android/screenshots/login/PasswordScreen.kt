package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.Screen

class PasswordScreen : Screen(PASSWORD_FIELD) {
    companion object {
        const val PASSWORD_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
    }

    fun proceedWith(password: String): MyStoreScreen {
        typeTextInto(PASSWORD_FIELD, password)
        clickOn(NEXT_BUTTON)

        return MyStoreScreen()
    }
}
