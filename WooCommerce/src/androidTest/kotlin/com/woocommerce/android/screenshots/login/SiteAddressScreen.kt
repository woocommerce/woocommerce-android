package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SiteAddressScreen : Screen(SITE_ADDRESS_FIELD) {
    companion object {
        const val SITE_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
    }

    fun proceedWith(siteAddress: String): EmailAddressScreen {
        typeTextInto(SITE_ADDRESS_FIELD, siteAddress)
        clickOn(NEXT_BUTTON)

        return EmailAddressScreen()
    }
}
