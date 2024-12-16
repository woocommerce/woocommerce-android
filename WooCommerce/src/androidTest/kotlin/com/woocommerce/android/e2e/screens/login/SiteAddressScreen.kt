package com.woocommerce.android.e2e.screens.login

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class SiteAddressScreen : Screen {
    constructor() : super(org.wordpress.android.login.R.id.input)

    companion object {
        val ERROR_MESSAGE = R.string.login_not_wordpress_site_v2
    }

    fun proceedWith(siteAddress: String): EmailAddressScreen {
        clickOn(org.wordpress.android.login.R.id.input)
        typeTextInto(org.wordpress.android.login.R.id.input, siteAddress)
        clickOn(R.id.bottom_button)

        return EmailAddressScreen()
    }

    fun enterNonWPAddress(siteAddress: String): SiteAddressScreen {
        clickOn(org.wordpress.android.login.R.id.input)
        typeTextInto(org.wordpress.android.login.R.id.input, siteAddress)
        clickOn(R.id.bottom_button)

        return this
    }

    fun assertErrorElements(): SiteAddressScreen {
        Espresso.onView(ViewMatchers.withText(R.string.login_try_another_store))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText(R.string.login_try_another_account))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }
}
