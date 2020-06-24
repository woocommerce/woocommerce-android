package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class EmailAddressScreen : Screen {
    companion object {
        const val EMAIL_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
        const val LOGIN_WITH_SITE_CREDENTIALS = R.id.login_site_button
    }

    constructor() : super(EMAIL_ADDRESS_FIELD)

    fun proceedWith(emailAddress: String): MagicLinkScreen {
        typeTextInto(EMAIL_ADDRESS_FIELD, emailAddress)
        clickOn(NEXT_BUTTON)

        return MagicLinkScreen()
    }

    fun proceedWithSiteLogin(): SiteCredentialsScreen {
        clickOn(LOGIN_WITH_SITE_CREDENTIALS)

        return SiteCredentialsScreen()
    }
}
