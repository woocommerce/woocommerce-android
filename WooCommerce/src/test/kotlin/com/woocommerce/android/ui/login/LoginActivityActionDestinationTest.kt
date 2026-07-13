package com.woocommerce.android.ui.login

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class LoginActivityActionDestinationTest {
    @Test
    fun `given site address action with nonblank site address, when resolving login action, then show prefilled site address`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_SITE_ADDRESS_ACTION,
            email = null,
            siteAddress = SITE_ADDRESS
        )

        assertThat(destination).isEqualTo(
            LoginActivityActionDestination.ShowSiteAddress(siteAddress = SITE_ADDRESS)
        )
    }

    @Test
    fun `given site address action with blank site address, when resolving login action, then show empty site address`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_SITE_ADDRESS_ACTION,
            email = null,
            siteAddress = " "
        )

        assertThat(destination).isEqualTo(
            LoginActivityActionDestination.ShowSiteAddress(siteAddress = null)
        )
    }

    @Test
    fun `given site address action with missing site address, when resolving login action, then show empty site address`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_SITE_ADDRESS_ACTION,
            email = null,
            siteAddress = null
        )

        assertThat(destination).isEqualTo(
            LoginActivityActionDestination.ShowSiteAddress(siteAddress = null)
        )
    }

    @Test
    fun `given WPCom email action with nonblank email, when resolving login action, then continue with email`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_WPCOM_EMAIL_ACTION,
            email = EMAIL,
            siteAddress = SITE_ADDRESS
        )

        assertThat(destination).isEqualTo(
            LoginActivityActionDestination.ContinueWithWPComEmail(EMAIL)
        )
    }

    @Test
    fun `given WPCom email action with blank email, when resolving login action, then start WPCom email login`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_WPCOM_EMAIL_ACTION,
            email = " ",
            siteAddress = SITE_ADDRESS
        )

        assertThat(destination).isEqualTo(LoginActivityActionDestination.StartWPComEmailLogin)
    }

    @Test
    fun `given WPCom email action with missing email, when resolving login action, then start WPCom email login`() {
        val destination = resolveLoginActivityActionDestination(
            action = LoginActivity.LOGIN_WITH_WPCOM_EMAIL_ACTION,
            email = null,
            siteAddress = SITE_ADDRESS
        )

        assertThat(destination).isEqualTo(LoginActivityActionDestination.StartWPComEmailLogin)
    }

    private companion object {
        const val EMAIL = "merchant@example.com"
        const val SITE_ADDRESS = "example.com"
    }
}
