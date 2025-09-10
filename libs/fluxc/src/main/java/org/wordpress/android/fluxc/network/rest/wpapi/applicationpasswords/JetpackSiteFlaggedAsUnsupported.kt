package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

data class JetpackSiteFlaggedAsUnsupported(
    val flow: Flow,
    val cause: Cause,
    val error: WPAPINetworkError
) {
    enum class Flow {
        APP_PASSWORD_GENERATION,
        API_REQUEST
    }

    enum class Cause {
        MAJOR_ERROR,
        GENERAL_FAILURES_THRESHOLD_REACHED
    }
}
