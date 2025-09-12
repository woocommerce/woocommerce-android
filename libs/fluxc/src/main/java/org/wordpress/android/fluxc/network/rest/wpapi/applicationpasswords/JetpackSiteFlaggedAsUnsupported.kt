package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

data class JetpackSiteFlaggedAsUnsupported(
    val scenario: Scenario,
    val cause: Cause,
    val apiErrorCode: String?,
    val httpStatusCode: Int?,
) {
    enum class Scenario {
        APP_PASSWORD_GENERATION,
        API_REQUEST
    }

    enum class Cause {
        MAJOR_ERROR,
        GENERAL_FAILURES_THRESHOLD_REACHED
    }
}
