package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
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
    ): WPAPIResponse<T> = site.getDelegate()
        .executeGetGsonRequest(
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

    override suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> = site.getDelegate()
        .executePostGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            body = body
        )

    override suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> = site.getDelegate()
        .executePutGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            body = body
        )

    override suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>
    ): WPAPIResponse<T> = site.getDelegate()
        .executeDeleteGsonRequest(
            site = site,
            path = path,
            clazz = clazz,
            params = params
        )

    private fun SiteModel.getDelegate() = when (origin) {
        SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork
        SiteModel.ORIGIN_XMLRPC, SiteModel.ORIGIN_WPAPI -> applicationPasswordsNetwork
        else -> error("Site with unsupported origin")
    }
}
