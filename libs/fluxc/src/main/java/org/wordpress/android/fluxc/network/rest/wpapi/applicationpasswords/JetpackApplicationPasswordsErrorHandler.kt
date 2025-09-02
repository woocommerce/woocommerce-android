package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackApplicationPasswordsErrorHandler @Inject constructor(
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport
) {
    private var failuresCount: MutableMap<Long, Int> = mutableMapOf()

    fun handleError(siteModel: SiteModel, error: WPAPINetworkError) {
        val httpStatusCode = error.volleyError?.networkResponse?.statusCode
        val apiErrorCode = error.errorCode

        if (httpStatusCode == UNAUTHORIZED ||
            httpStatusCode == FORBIDDEN ||
            httpStatusCode == TOO_MANY_REQUESTS ||
            apiErrorCode == "incorrect_password" ||
            apiErrorCode == "application_passwords_disabled_for_user" ||
            apiErrorCode == ApplicationPasswordsNetwork.APPLICATION_PASSWORDS_NOT_SUPPORT_ERROR_CODE
        ) {
            AppLog.w(
                AppLog.T.API,
                "Disabling Jetpack Application Passwords support for site ${siteModel.siteId} " +
                    "due to error: $httpStatusCode / $apiErrorCode"
            )
            jetpackApplicationPasswordsSupport.flagAsUnsupported(siteModel)
        } else {
            val siteFailuresCount = (failuresCount[siteModel.siteId] ?: 0) + 1
            failuresCount[siteModel.siteId] = siteFailuresCount

            if (siteFailuresCount >= FAILURES_THRESHOLD) {
                AppLog.w(
                    AppLog.T.API,
                    "Disabling Jetpack Application Passwords support for site ${siteModel.siteId} " +
                        "after $failuresCount failures"
                )
                jetpackApplicationPasswordsSupport.flagAsUnsupported(siteModel)
            }
        }
    }

    companion object {
        private const val UNAUTHORIZED = 401
        private const val FORBIDDEN = 403
        private const val TOO_MANY_REQUESTS = 429
        private const val FAILURES_THRESHOLD = 10
    }
}
