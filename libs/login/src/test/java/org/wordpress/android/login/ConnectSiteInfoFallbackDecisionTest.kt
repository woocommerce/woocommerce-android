package org.wordpress.android.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR
import org.wordpress.android.fluxc.store.SiteStore.WPAPIDiscoveryResult

class ConnectSiteInfoFallbackDecisionTest {
    @Test
    fun `given null error, when creating fallback decision, then not applicable`() {
        val decision = ConnectSiteInfoFallbackDecision.from(null, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.NOT_APPLICABLE)
    }

    @Test
    fun `given error without discovery state, when creating fallback decision, then not applicable`() {
        val error = SiteError(INVALID_SITE)

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.NOT_APPLICABLE)
    }

    @Test
    fun `given discovery state and non-Woo login mode, when creating fallback decision, then not applicable`() {
        val error = SiteError(INVALID_SITE, wpApiDiscovery = WPAPIDiscoveryResult())

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.FULL)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.NOT_APPLICABLE)
    }

    @Test
    fun `given discovered WP API base URL in Woo login mode, when creating fallback decision, then offer site credentials`() {
        val error = SiteError(
            INVALID_SITE,
            wpApiDiscovery = WPAPIDiscoveryResult(wpApiBaseUrl = "https://example.com/wp-json/")
        )

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.OFFER_SITE_CREDENTIALS)
    }

    @Test
    fun `given discovered connectivity error in Woo login mode, when creating fallback decision, then offer site credentials`() {
        val error = SiteError(
            WORDPRESS_COM_CONNECTIVITY_ERROR,
            wpApiDiscovery = WPAPIDiscoveryResult(wpApiBaseUrl = "https://example.com/wp-json/")
        )

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.OFFER_SITE_CREDENTIALS)
    }

    @Test
    fun `given invalid site discovery without WP API base URL in Woo login mode, when creating decision, then show original error`() {
        val error = SiteError(INVALID_SITE, wpApiDiscovery = WPAPIDiscoveryResult())

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.SHOW_ORIGINAL_ERROR)
    }

    @Test
    fun `given connectivity discovery without WP API base URL in Woo login mode, when creating decision, then show original error`() {
        val error = SiteError(WORDPRESS_COM_CONNECTIVITY_ERROR, wpApiDiscovery = WPAPIDiscoveryResult())

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.SHOW_ORIGINAL_ERROR)
    }
}
