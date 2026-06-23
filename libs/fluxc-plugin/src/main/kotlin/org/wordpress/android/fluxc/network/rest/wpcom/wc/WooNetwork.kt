package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkingMode
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.util.AppLog
import java.security.GeneralSecurityException
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Woo app supports connecting to sites using either Jetpack or the site credentials (technically application
 * passwords). This class allows this by supporting multiple networking implementations depending on the type of site:
 * - Jetpack Sites:
 *     - When enabled and if the site supports it, the API call will use [ApplicationPasswordsNetwork]
 *     - Otherwise, it will use [JetpackTunnelWPAPINetwork]
 * - Non-Jetpack Sites: the API call will use Application Passwords using [ApplicationPasswordsNetwork]
 *
 * The [SiteModel.ORIGIN_XMLRPC] support is kept for backward compatibility
 */
@Singleton
class WooNetwork @Inject constructor(
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork,
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport,
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler,
    private val unknownBlogListener: Optional<UnknownBlogListener>,
    private val invalidSignatureListener: Optional<InvalidSignatureListener>
) : WPAPINetwork {
    override suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        forced: Boolean,
        requestTimeout: Int,
        retries: Int
    ): WPAPIResponse<T> = handleRequest(site, RequestContext(path, "GET")) {
        executeGetGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            params = params,
            enableCaching = enableCaching,
            cacheTimeToLive = cacheTimeToLive,
            forced = forced,
            requestTimeout = requestTimeout,
            retries = retries
        )
    }

    override suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> = handleRequest(site, RequestContext(path, "POST")) {
        executePostGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            body = body
        )
    }

    override suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> = handleRequest(site, RequestContext(path, "PUT")) {
        executePutGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            body = body
        )
    }

    override suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>
    ): WPAPIResponse<T> = handleRequest(site, RequestContext(path, "DELETE")) {
        executeDeleteGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            params = params
        )
    }

    private suspend fun <T : Any> handleRequest(
        site: SiteModel,
        requestContext: RequestContext,
        request: suspend WPAPINetwork.() -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        val response = when (site.origin) {
            SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC -> {
                applicationPasswordsNetwork.request().copyWith(
                    networkingMode = WPAPINetworkingMode.ApplicationPasswords
                )
            }

            SiteModel.ORIGIN_WPCOM_REST -> {
                handleRequestForJetpackSites(site, requestContext) {
                    request()
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported site origin: ${site.origin}")
            }
        }
        notifyIfUnknownBlog(site, response)
        notifyInvalidSignatureListener(site, response)
        return response
    }

    private fun <T : Any> notifyIfUnknownBlog(site: SiteModel, response: WPAPIResponse<T>) {
        if (response is WPAPIResponse.Error && response.error.errorCode == UNKNOWN_BLOG_ERROR_CODE) {
            unknownBlogListener.ifPresent { it.onUnknownBlog(site.siteId) }
        }
    }

    private fun <T : Any> notifyInvalidSignatureListener(site: SiteModel, response: WPAPIResponse<T>) {
        val listener = invalidSignatureListener.orElse(null) ?: return
        when (response) {
            is WPAPIResponse.Error -> {
                if (response.error.errorCode == WooError.REST_INVALID_SIGNATURE_CODE) {
                    listener.onInvalidSignatureDetected(site)
                }
            }

            is WPAPIResponse.Success -> listener.onSuccessfulConnection(site)
        }
    }

    private suspend fun <T : Any> handleRequestForJetpackSites(
        site: SiteModel,
        requestContext: RequestContext,
        request: suspend WPAPINetwork.() -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        val appPasswordsEnabled = applicationPasswordsConfiguration.isEnabledForJetpackAccess()
        if (!appPasswordsEnabled || !jetpackApplicationPasswordsSupport.supportsAppPasswords(site)) {
            if (appPasswordsEnabled) {
                AppLog.v(
                    AppLog.T.API,
                    "Application Passwords not supported for site: ${site.url}, use Jetpack Tunnel"
                )
            }
            return jetpackTunnelWPAPINetwork.request().copyWith(
                networkingMode = WPAPINetworkingMode.JetpackTunnel()
            )
        }

        val appPasswordsResponse = try {
            applicationPasswordsNetwork.request()
        } catch (e: GeneralSecurityException) {
            AppLog.e(AppLog.T.API, "Error setting up Application Passwords encryption", e)
            WPAPIResponse.Error(
                error = WPAPINetworkError(
                    baseError = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
                    errorCode = ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
                )
            )
        }

        return when (appPasswordsResponse) {
            is WPAPIResponse.Success<*> -> {
                (appPasswordsResponse as WPAPIResponse<T>).copyWith(
                    networkingMode = WPAPINetworkingMode.ApplicationPasswordsWithJetpack
                )
            }

            is WPAPIResponse.Error<*> -> {
                logMessageForFailedRequest(requestContext, site.url, appPasswordsResponse.error)

                jetpackTunnelWPAPINetwork.request().also {
                    if (it is WPAPIResponse.Success) {
                        // when the fallback to Jetpack Tunnel succeeds, notify the error handler
                        jetpackApplicationPasswordsErrorHandler.handleError(site, appPasswordsResponse.error)
                    }
                }.copyWith(
                    networkingMode = WPAPINetworkingMode.JetpackTunnel(
                        isFallback = true,
                        applicationPasswordsError = appPasswordsResponse.error
                    )
                )
            }
        }
    }

    private fun <T : Any> WPAPIResponse<T>.copyWith(
        networkingMode: WPAPINetworkingMode?
    ): WPAPIResponse<T> {
        return when (this) {
            is WPAPIResponse.Success -> this.copy(networkingMode = networkingMode)
            is WPAPIResponse.Error -> this.copy(networkingMode = networkingMode)
        }
    }

    private fun logMessageForFailedRequest(requestContext: RequestContext, siteUrl: String, error: WPAPINetworkError) {
        AppLog.w(
            AppLog.T.API,
            "Request failed using Application Passwords for Jetpack Site,\n" +
                "site: $siteUrl,\n" +
                "path: ${requestContext.path},\n" +
                "method: ${requestContext.method},\n" +
                "error: HTTP status code ${error.volleyError?.networkResponse?.statusCode}, " +
                "error message: ${error.errorCode?.ifEmpty { null } ?: error.message}",
        )
    }

    private data class RequestContext(
        val path: String,
        val method: String
    )

    companion object {
        private const val UNKNOWN_BLOG_ERROR_CODE = "unknown_blog"
    }
}
