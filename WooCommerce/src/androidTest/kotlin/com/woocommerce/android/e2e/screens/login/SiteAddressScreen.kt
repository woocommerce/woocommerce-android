package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class SiteAddressScreen : Screen {
    companion object {
        const val SITE_ADDRESS_FIELD = R.id.input
        const val NEXT_BUTTON = R.id.bottom_button
    }

    constructor() : super(SITE_ADDRESS_FIELD)

    fun proceedWith(siteAddress: String): EmailAddressScreen {
        typeTextInto(SITE_ADDRESS_FIELD, siteAddress)
        clickOn(NEXT_BUTTON)

        return EmailAddressScreen()
    }
}
