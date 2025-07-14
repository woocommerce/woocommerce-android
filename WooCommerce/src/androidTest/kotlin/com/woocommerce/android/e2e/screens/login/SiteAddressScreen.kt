package com.woocommerce.android.e2e.screens.login

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class SiteAddressScreen : Screen {
    constructor() : super(org.wordpress.android.login.R.id.input)

    fun proceedWith(siteAddress: String): EmailAddressScreen {
        clickOn(org.wordpress.android.login.R.id.input)
        typeTextInto(org.wordpress.android.login.R.id.input, siteAddress)
        clickOn(R.id.bottom_button)

        return EmailAddressScreen()
    }
}
