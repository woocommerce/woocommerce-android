package com.woocommerce.android.screenshots.login

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SiteAddressScreen : Screen {
    companion object {
        const val SITE_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.primary_button
        const val INPUT_ERROR = R.id.textinput_error
        const val MESSAGE = "Check that the site URL entered is valid"
    }

    constructor() : super(SITE_ADDRESS_FIELD)

    fun proceedWith(siteAddress: String): EmailAddressScreen {
        typeTextInto(SITE_ADDRESS_FIELD, siteAddress)
        clickOn(NEXT_BUTTON)

        return EmailAddressScreen()
    }
}
