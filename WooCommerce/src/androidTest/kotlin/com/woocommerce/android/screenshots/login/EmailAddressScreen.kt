package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class EmailAddressScreen : Screen(EMAIL_ADDRESS_FIELD) {
    companion object {
        const val EMAIL_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
        const val LOGIN_WITH_SITE_CREDENTIALS_BUTTON = R.id.login_site_button
    }

    fun proceedWith(emailAddress: String): MagicLinkScreen {
        typeOps.typeTextInto(EMAIL_ADDRESS_FIELD, emailAddress)
        clickOps.clickOn(NEXT_BUTTON)

        return MagicLinkScreen()
    }

    fun proceedWithSiteLogin(): SiteCredentialsScreen {
        clickOps.clickOn(LOGIN_WITH_SITE_CREDENTIALS_BUTTON)

        return SiteCredentialsScreen()
    }
}
