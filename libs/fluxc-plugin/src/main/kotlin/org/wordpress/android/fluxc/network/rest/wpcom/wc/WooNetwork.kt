package org.wordpress.android.fluxc.network.rest.wpcom.wc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkingMode
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.util.AppLog
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
    private val siteSqlUtils: SiteSqlUtils
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
        return when (site.origin) {
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
    }

    private suspend fun <T : Any> handleRequestForJetpackSites(
        site: SiteModel,
        requestContext: RequestContext,
        request: suspend WPAPINetwork.() -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        val appPasswordsEnabled = applicationPasswordsConfiguration.isEnabledForJetpackAccess()
        if (!appPasswordsEnabled || !site.isApplicationPasswordsSupported) {
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

        return when (val appPasswordsResponse = applicationPasswordsNetwork.request()) {
            is WPAPIResponse.Success<*> -> {
                (appPasswordsResponse as WPAPIResponse<T>).copyWith(
                    networkingMode = WPAPINetworkingMode.ApplicationPasswordsWithJetpack
                )
            }

            is WPAPIResponse.Error<*> -> {
                logMessageForFailedRequest(requestContext, site.url, appPasswordsResponse.error)

                if (appPasswordsResponse.error.errorCode ==
                    ApplicationPasswordsNetwork.APPLICATION_PASSWORDS_NOT_SUPPORT_ERROR_CODE
                ) {
                    withContext(Dispatchers.IO) {
                        site.applicationPasswordsAuthorizeUrl = null
                        siteSqlUtils.insertOrUpdateSite(site)
                    }
                }
                jetpackTunnelWPAPINetwork.request().copyWith(
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
}
