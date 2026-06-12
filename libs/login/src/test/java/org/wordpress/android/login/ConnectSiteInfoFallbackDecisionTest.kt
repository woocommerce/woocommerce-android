package org.wordpress.android.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.INVALID_SITE
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
    fun `given discovery state without WP API base URL in Woo login mode, when creating fallback decision, then show error`() {
        val error = SiteError(INVALID_SITE, wpApiDiscovery = WPAPIDiscoveryResult())

        val decision = ConnectSiteInfoFallbackDecision.from(error, LoginMode.WOO_LOGIN_MODE)

        assertThat(decision).isEqualTo(ConnectSiteInfoFallbackDecision.SHOW_CONNECTION_ERROR)
    }
}
