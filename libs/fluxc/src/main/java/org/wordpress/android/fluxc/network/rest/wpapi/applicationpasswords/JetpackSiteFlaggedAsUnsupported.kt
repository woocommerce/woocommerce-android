package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

data class JetpackSiteFlaggedAsUnsupported(
    val flow: Flow,
    val cause: Cause,
    val apiErrorCode: String?,
    val httpStatusCode: Int?,
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
