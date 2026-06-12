package org.wordpress.android.login

import org.wordpress.android.fluxc.store.SiteStore.SiteError

enum class ConnectSiteInfoFallbackDecision {
    OFFER_SITE_CREDENTIALS,
    SHOW_CONNECTION_ERROR,
    NOT_APPLICABLE;

    companion object {
        @JvmStatic
        fun from(error: SiteError?, loginMode: LoginMode?): ConnectSiteInfoFallbackDecision {
            val discovery = error?.wpApiDiscovery ?: return NOT_APPLICABLE
            if (loginMode != LoginMode.WOO_LOGIN_MODE) return NOT_APPLICABLE
            return if (discovery.wpApiBaseUrl != null) OFFER_SITE_CREDENTIALS else SHOW_CONNECTION_ERROR
        }
    }
}
