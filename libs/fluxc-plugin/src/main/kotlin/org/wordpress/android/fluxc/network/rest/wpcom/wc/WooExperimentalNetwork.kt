package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkingMode
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An experimental network class for WooCommerce that allows using Application Passwords for Jetpack sites too,
 * it will allow us to confirm if the Jetpack Tunnel is causing performance issues.
 *
 * Depending on the results of this experiment, we will either move the logic to the existing WooNetwork class or
 * drop the experiment.
 */
@Singleton
class WooExperimentalNetwork @Inject constructor(
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork,
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val dispatcher: Dispatcher
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
    ): WPAPIResponse<T> = handleRequest(site) {
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
    ): WPAPIResponse<T> = handleRequest(site) {
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
    ): WPAPIResponse<T> = handleRequest(site) {
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
    ): WPAPIResponse<T> = handleRequest(site) {
        executeDeleteGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            params = params
        )
    }

    private suspend fun <T : Any> handleRequest(
        site: SiteModel,
        request: suspend WPAPINetwork.() -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPAPI, SiteModel.ORIGIN_XMLRPC -> {
                applicationPasswordsNetwork.request().copyWith(
                    networkingMode = WPAPINetworkingMode.ApplicationPasswords
                )
            }

            SiteModel.ORIGIN_WPCOM_REST -> {
                handleRequestForJetpackSites(site) {
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
        request: suspend WPAPINetwork.() -> WPAPIResponse<T>
    ): WPAPIResponse<T> {
        if (!site.isApplicationPasswordsSupported) {
            AppLog.v(
                AppLog.T.API,
                "Application Passwords not supported for site: ${site.url}, use Jetpack Tunnel"
            )
            return jetpackTunnelWPAPINetwork.request().copyWith(
                networkingMode = WPAPINetworkingMode.JetpackTunnel()
            )
        }

        val hasAppPassword = applicationPasswordsStore.hasCredentials(site)

        return when (val appPasswordsResponse = applicationPasswordsNetwork.request()) {
            is WPAPIResponse.Success<*> -> {
                AppLog.v(
                    AppLog.T.API,
                    "Request successful for site: ${site.url}, using Application Passwords"
                )
                (appPasswordsResponse as WPAPIResponse<T>).copyWith(
                    // When creating a new Application Password, we don't want to track this request, as its duration
                    // is not relevant to our experiment.
                    // So we track only requests where we already have a password saved.
                    networkingMode = if (hasAppPassword) WPAPINetworkingMode.ApplicationPasswordsWithJetpack else null
                )
            }

            is WPAPIResponse.Error<*> -> {
                AppLog.w(
                    AppLog.T.API,
                    "Request failed for site: ${site.url} using Application Passwords, falling back to Jetpack Tunnel"
                )
                if (appPasswordsResponse.error.errorCode ==
                    ApplicationPasswordsNetwork.APPLICATION_PASSWORDS_NOT_SUPPORT_ERROR_CODE
                ) {
                    site.applicationPasswordsAuthorizeUrl = null
                    dispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site))
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
}
