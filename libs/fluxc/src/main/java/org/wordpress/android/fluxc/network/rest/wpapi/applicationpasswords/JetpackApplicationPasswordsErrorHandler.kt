package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackApplicationPasswordsErrorHandler @Inject constructor(
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport
) {
    private var failuresCount: Int = 0

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
            jetpackApplicationPasswordsSupport.flagAsUnsupported(siteModel)
        } else {
            failuresCount++
            if (failuresCount >= FAILURES_THRESHOLD) {
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
