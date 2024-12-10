package com.woocommerce.android.e2e.screens.login

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class LoginNotWPScreen : Screen {
    companion object {
        val ERROR_MESSAGE = R.string.login_not_wordpress_site_v2
    }
    constructor() : super(ERROR_MESSAGE)

    fun assertErrorElements(): LoginNotWPScreen {
        Espresso.onView(ViewMatchers.withText(R.string.login_try_another_store))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText(R.string.login_try_another_account))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }
}
