package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class CookieNonceAuthenticator @Inject constructor(
    private val nonceRestClient: NonceRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteStore: SiteStore,
    private val coroutineEngine: CoroutineEngine,
    private val httpsUrlNormalizer: HttpsUrlNormalizer,
) {
    suspend fun authenticate(
        siteUrl: String,
        username: String,
        password: String
    ): CookieNonceAuthenticationResult = authenticate(
        endpoints = CookieNonceAuthenticationEndpoints(siteUrl),
        username = username,
        password = password
    )

    suspend fun authenticate(
        endpoints: CookieNonceAuthenticationEndpoints,
        username: String,
        password: String
    ): CookieNonceAuthenticationResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "authenticate") {
            val resolvedEndpoints = endpoints.normalizedToHttps()
            when (val nonce = nonceRestClient.requestNonce(resolvedEndpoints, username, password)) {
                is Available -> CookieNonceAuthenticationResult.Success
                is FailedRequest -> {
                    CookieNonceAuthenticationResult.Error(
                        type = nonce.type,
                        message = nonce.errorMessage,
                        networkError = nonce.networkError,
                        loginEntryVerified = nonce.loginEntryVerified
                    )
                }

                is Unknown -> CookieNonceAuthenticationResult.Error(
                    type = Nonce.CookieNonceErrorType.UNKNOWN,
                    loginEntryVerified = nonce.loginEntryVerified
                )
            }
        }
    }

    suspend fun <T> makeAuthenticatedWPAPIRequest(
        site: SiteModel,
        fetchMethod: suspend (Available) -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        val usingSavedRestUrl = site.wpApiRestUrl != null
        if (!usingSavedRestUrl) {
            val normalizedSiteUrl = httpsUrlNormalizer.normalize(site.url, addHttpsSchemeIfMissing = true).normalizedUrl
            site.wpApiRestUrl = httpsUrlNormalizer.normalize(discoverApiEndpoint(normalizedSiteUrl)).normalizedUrl
            try {
                siteStore.insertOrUpdateSite(site)
            } catch (e: SiteStorePersistence.DuplicateSiteException) {
                AppLog.w(AppLog.T.API, "Duplicate site detected while saving wpApiRestUrl: ${e.message}")
            }
        }

        val response = makeAuthenticatedWPAPIRequest(
            endpoints = CookieNonceAuthenticationEndpoints.from(site),
            wpApiUrl = site.wpApiRestUrl,
            username = site.username,
            password = site.password
        ) { _, nonce ->
            fetchMethod(nonce)
        }

        return if (response is WPAPIResponse.Error<*> &&
            response.error.volleyError?.networkResponse?.statusCode == STATUS_CODE_NOT_FOUND) {
            // call failed with 'not found' so clear the (failing) rest url
            site.wpApiRestUrl = null
            try {
                siteStore.insertOrUpdateSite(site)
            } catch (e: SiteStorePersistence.DuplicateSiteException) {
                AppLog.w(AppLog.T.API, "Duplicate site detected while clearing wpApiRestUrl: ${e.message}")
            }

            if (usingSavedRestUrl) {
                // If we did the previous call with a saved rest url, try again by making
                // recursive call. This time there is no saved rest url to use
                // so the rest url will be retrieved using discovery
                makeAuthenticatedWPAPIRequest(site, fetchMethod)
            } else {
                // Already used discovery to fetch the rest base url and still got 'not found', so
                // just return the error response
                response
            }
        } else response
    }

    private suspend fun <T> makeAuthenticatedWPAPIRequest(
        endpoints: CookieNonceAuthenticationEndpoints,
        wpApiUrl: String,
        username: String,
        password: String,
        fetchMethod: suspend (wpApiUrl: String, nonce: Available) -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        val normalizedEndpoints = endpoints.normalizedToHttps()
        val normalizedWpApiUrl = httpsUrlNormalizer.normalize(wpApiUrl).normalizedUrl
        var nonce = nonceRestClient.getNonce(normalizedEndpoints.siteUrl, username)
        val usingSavedNonce = nonce is Available
        if (nonce !is Available) {
            nonce = nonceRestClient.requestNonce(normalizedEndpoints, username, password)
        }
        if (nonce !is Available) return nonce.toErrorResponse()

        val response = fetchMethod(normalizedWpApiUrl, nonce)

        if (response is WPAPIResponse.Success<*>) return response

        val error = (response as WPAPIResponse.Error<*>).error
        val statusCode = error.volleyError?.networkResponse?.statusCode
        val errorCode = (error as? WPAPINetworkError)?.errorCode
        return when {
            statusCode == STATUS_CODE_UNAUTHORIZED ||
                (statusCode == STATUS_CODE_FORBIDDEN && errorCode == "rest_cookie_invalid_nonce") -> {
                if (usingSavedNonce) {
                    // Call with saved nonce failed, so try getting a new one
                    val previousNonce = nonce
                    val newNonce = nonceRestClient.requestNonce(normalizedEndpoints, username, password)

                    // Try original call again if we have a new nonce
                    when {
                        newNonce !is Available -> newNonce.toErrorResponse()
                        newNonce != previousNonce -> fetchMethod(normalizedWpApiUrl, newNonce)
                        else -> response
                    }
                } else {
                    response
                }
            }
            // For all other failures just return the error response
            else -> response
        }
    }

    private fun discoverApiEndpoint(
        url: String
    ): String {
        return discoveryWPAPIRestClient.discoverWPAPIBaseURL(url) // discover rest api endpoint
            ?: WPAPIDiscoveryUtils.buildDefaultRESTBaseUrl(url)
    }

    private fun CookieNonceAuthenticationEndpoints.normalizedToHttps() = copy(
        siteUrl = httpsUrlNormalizer.normalize(siteUrl, addHttpsSchemeIfMissing = true).normalizedUrl,
        loginEntryUrl = loginEntryUrl?.let { httpsUrlNormalizer.normalize(it).normalizedUrl },
        adminBaseUrl = adminBaseUrl?.let { httpsUrlNormalizer.normalize(it).normalizedUrl },
    )

    private fun <T> Nonce.toErrorResponse(): WPAPIResponse.Error<T> {
        val (genericErrorType, message) = when (this) {
            is FailedRequest -> Pair(
                networkError?.type ?: GenericErrorType.NOT_AUTHENTICATED,
                errorMessage?.takeUnless(String::isBlank)
                    ?: networkError?.message?.takeUnless(String::isBlank)
                    ?: "Cookie nonce acquisition failed: ${type.name}"
            )
            is Unknown -> Pair(
                GenericErrorType.NETWORK_ERROR,
                "Cookie nonce acquisition failed: ${Nonce.CookieNonceErrorType.UNKNOWN.name}"
            )
            is Available -> error("An available nonce cannot be converted to an error response")
        }
        return WPAPIResponse.Error(WPAPINetworkError(BaseNetworkError(genericErrorType, message)))
    }

    sealed interface CookieNonceAuthenticationResult {
        object Success : CookieNonceAuthenticationResult
        data class Error(
            val type: Nonce.CookieNonceErrorType,
            val message: String? = null,
            val networkError: BaseNetworkError? = null,
            val loginEntryVerified: Boolean = false,
        ) : CookieNonceAuthenticationResult
    }

    companion object {
        private const val STATUS_CODE_NOT_FOUND = 404
        private const val STATUS_CODE_FORBIDDEN = 403
        private const val STATUS_CODE_UNAUTHORIZED = 401
    }
}
