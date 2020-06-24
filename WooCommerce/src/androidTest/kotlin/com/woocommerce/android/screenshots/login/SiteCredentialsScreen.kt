package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SiteCredentialsScreen : Screen {
    companion object {
        const val USERNAME_ADDRESS_FIELD = "Username"
        const val PASSWORD_ADDRESS_FIELD = "Password"
        const val NEXT_BUTTON = R.id.primary_button
        const val SITE_TITLE = R.id.login_site_title_static
    }

    constructor() : super(SITE_TITLE)

    fun proceedWith(username: String, password: String): MagicLinkScreen {
        typeTextInto(USERNAME_ADDRESS_FIELD, username)
        typeTextInto(PASSWORD_ADDRESS_FIELD, password)
        clickOn(NEXT_BUTTON)

        return MagicLinkScreen()
    }
}
