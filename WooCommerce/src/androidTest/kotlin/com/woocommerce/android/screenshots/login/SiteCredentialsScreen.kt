package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SiteCredentialsScreen : Screen(SITE_TITLE) {
    companion object {
        const val TEXT_INPUT = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
        const val SITE_TITLE = R.id.login_site_title_static
    }

    fun proceedWith(username: String, password: String): MagicLinkScreen {
        typeOps.typeTextInto(TEXT_INPUT, "Username", username)
        typeOps.typeTextInto(TEXT_INPUT, "Password", password)
        clickOps.clickOn(NEXT_BUTTON)

        return MagicLinkScreen()
    }
}
