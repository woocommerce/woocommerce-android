package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackApplicationPasswordsErrorHandler @Inject constructor(
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport,
    private val appLogWrapper: AppLogWrapper,
    private val listener: Optional<ApplicationPasswordsListener>
) {
    private var failuresCount: MutableMap<Long, Int> = mutableMapOf()

    @Suppress("ComplexCondition")
    fun handleError(siteModel: SiteModel, error: WPAPINetworkError) {
        if (!jetpackApplicationPasswordsSupport.supportsAppPasswords(siteModel)) {
            // Site already flagged
            return
        }

        val isAppPasswordsGenerationError = error.errorCode?.startsWith(
            ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX
        ) == true

        val httpStatusCode = error.volleyError?.networkResponse?.statusCode
        val apiErrorCode = if (isAppPasswordsGenerationError) {
            error.errorCode.removePrefix(ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX)
        } else {
            error.errorCode
        }

        if (httpStatusCode == UNAUTHORIZED ||
            httpStatusCode == FORBIDDEN ||
            httpStatusCode == TOO_MANY_REQUESTS ||
            apiErrorCode == "incorrect_password" ||
            apiErrorCode == "application_passwords_disabled_for_user"
        ) {
            appLogWrapper.w(
                AppLog.T.API,
                "Disabling Jetpack Application Passwords support for site ${siteModel.siteId} " +
                    "due to error: $httpStatusCode / $apiErrorCode"
            )
            jetpackApplicationPasswordsSupport.flagAsUnsupported(siteModel)
            trackFlaggingEvent(
                isAppPasswordsGenerationError = isAppPasswordsGenerationError,
                cause = JetpackSiteFlaggedAsUnsupported.Cause.MAJOR_ERROR,
                error = error
            )
        } else {
            val siteFailuresCount = (failuresCount[siteModel.siteId] ?: 0) + 1
            failuresCount[siteModel.siteId] = siteFailuresCount

            if (siteFailuresCount >= FAILURES_THRESHOLD) {
                appLogWrapper.w(
                    AppLog.T.API,
                    "Disabling Jetpack Application Passwords support for site ${siteModel.siteId} " +
                        "after $siteFailuresCount failures"
                )
                jetpackApplicationPasswordsSupport.flagAsUnsupported(siteModel)
                trackFlaggingEvent(
                    isAppPasswordsGenerationError = isAppPasswordsGenerationError,
                    cause = JetpackSiteFlaggedAsUnsupported.Cause.GENERAL_FAILURES_THRESHOLD_REACHED,
                    error = error
                )
            }
        }
    }

    private fun trackFlaggingEvent(
        isAppPasswordsGenerationError: Boolean,
        cause: JetpackSiteFlaggedAsUnsupported.Cause,
        error: WPAPINetworkError
    ) {
        if (listener.isPresent) {
            listener.get().onJetpackSiteFlaggedAsUnsupported(
                JetpackSiteFlaggedAsUnsupported(
                    flow = if (isAppPasswordsGenerationError) {
                        JetpackSiteFlaggedAsUnsupported.Flow.APP_PASSWORD_GENERATION
                    } else {
                        JetpackSiteFlaggedAsUnsupported.Flow.API_REQUEST
                    },
                    cause = cause,
                    error = error
                )
            )
        }
    }

    companion object {
        private const val UNAUTHORIZED = 401
        private const val FORBIDDEN = 403
        private const val TOO_MANY_REQUESTS = 429
        private const val FAILURES_THRESHOLD = 10
    }
}
