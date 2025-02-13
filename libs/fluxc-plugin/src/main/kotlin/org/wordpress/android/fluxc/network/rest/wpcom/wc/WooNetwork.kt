package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Woo app supports connecting to sites using either Jetpack or the site credentials (technically application
 * passwords). This class allows this by supporting multiple networking implementations depending on the type of site:
 * - Jetpack Sites: the API call will use Jetpack Tunnel using [JetpackTunnelWPAPINetwork]
 * - Non-Jetpack Sites: the API call will use Application Passwords using [ApplicationPasswordsNetwork]
 *
 * The [SiteModel.ORIGIN_XMLRPC] support is kept for backward compatibility
 */
@Singleton
class WooNetwork @Inject constructor(
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork
) {
    suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false,
        requestTimeout: Int = BaseRequest.DEFAULT_REQUEST_TIMEOUT,
        retries: Int = BaseRequest.DEFAULT_MAX_RETRIES
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executeGetGsonRequest(
                site, path, clazz, params, enableCaching, cacheTimeToLive, forced, requestTimeout, retries
            ).toWPAPIResponse()
            SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPAPI -> applicationPasswordsNetwork.executeGetGsonRequest(
                site, path, clazz, params, enableCaching, cacheTimeToLive, forced, requestTimeout, retries
            )
            else -> error("Site with unsupported origin")
        }
    }

    suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executePostGsonRequest(site, path, clazz, body)
                .toWPAPIResponse()
            SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPAPI -> applicationPasswordsNetwork.executePostGsonRequest(
                site, path, clazz, body
            )
            else -> error("Site with unsupported origin")
        }
    }

    suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executePutGsonRequest(site, path, clazz, body)
                .toWPAPIResponse()
            SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPAPI -> applicationPasswordsNetwork.executePutGsonRequest(
                site, path, clazz, body
            )
            else -> error("Site with unsupported origin")
        }
    }

    suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executeDeleteGsonRequest(site, path, clazz, params)
                .toWPAPIResponse()
            SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPAPI -> applicationPasswordsNetwork.executeDeleteGsonRequest(
                site, path, clazz, params
            )
            else -> error("Site with unsupported origin")
        }
    }
}

private fun <T> JetpackResponse<T>.toWPAPIResponse(): WPAPIResponse<T> {
    return when (this) {
        is JetpackSuccess -> WPAPIResponse.Success(data)
        is JetpackError -> WPAPIResponse.Error(WPAPINetworkError(error, errorCode = error.apiError))
    }
}
