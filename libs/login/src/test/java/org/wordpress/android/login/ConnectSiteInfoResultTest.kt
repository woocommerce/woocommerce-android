package org.wordpress.android.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectSiteInfoResultTest {
    @Test
    fun `given a WPCom site, when checking the login flow, then WPCom login is used`() {
        val result = connectSiteInfoResult(isWPCom = true)

        assertThat(result.shouldUseWPComLogin).isTrue
    }

    @Test
    fun `given a Commerce-garden site, when checking the login flow, then WPCom login is used`() {
        val result = connectSiteInfoResult(isCommerceGarden = true)

        assertThat(result.shouldUseWPComLogin).isTrue
    }

    @Test
    fun `given a self-hosted Jetpack-connected site, when checking the login flow, then WPCom login is used`() {
        val result = connectSiteInfoResult(isJetpackConnected = true)

        assertThat(result.shouldUseWPComLogin).isTrue
    }

    @Test
    fun `given a self-hosted site with Jetpack installed but not connected, then site credentials are used`() {
        val result = connectSiteInfoResult(hasJetpack = true, isJetpackConnected = false)

        assertThat(result.shouldUseWPComLogin).isFalse
    }

    @Test
    fun `given a self-hosted non-Jetpack site, when checking the login flow, then site credentials are used`() {
        val result = connectSiteInfoResult()

        assertThat(result.shouldUseWPComLogin).isFalse
    }

    @Test
    fun `given a WPCom-suspended site, when checking the login flow, then site credentials are used`() {
        val result = connectSiteInfoResult(isWPComSuspended = true)

        assertThat(result.shouldUseWPComLogin).isFalse
    }

    private fun connectSiteInfoResult(
        hasJetpack: Boolean = false,
        isWPComSuspended: Boolean = false,
        isWPCom: Boolean = false,
        isCommerceGarden: Boolean = false,
        isJetpackConnected: Boolean = false,
    ) = ConnectSiteInfoResult(
        url = "https://example.com",
        urlAfterRedirects = null,
        hasJetpack = hasJetpack,
        isWPComSuspended = isWPComSuspended,
        isWPCom = isWPCom,
        isCommerceGarden = isCommerceGarden,
        isJetpackConnected = isJetpackConnected,
    )
}
