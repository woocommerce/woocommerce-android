package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.login.SiteAddressScreen.Companion
import com.woocommerce.android.screenshots.util.Screen

class SiteCredentialsScreen : Screen {
    companion object {
        const val TEXT_INPUT = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
        const val SITE_TITLE = R.id.login_site_title_static
    }

    constructor() : super(SITE_TITLE)

    fun proceedWith(username: String, password: String): MagicLinkScreen {
        typeTextInto(TEXT_INPUT, "Username", username)
        typeTextInto(TEXT_INPUT, "Password", password)
        clickOn(NEXT_BUTTON)

        return MagicLinkScreen()
    }
}
