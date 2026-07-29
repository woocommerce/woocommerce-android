package org.wordpress.android.fluxc.network.rest.wpcom

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This class acts as a network engine for using Jetpack Tunnel to call WP API endpoints.
 * The goal of adding this class instead of directly inheriting from [BaseWPComRestClient] is allowing to move away
 * from the traditional model of inheritance, and allowing the feature RestClients to have multiple network
 * implementations at the same time.
 */
@Singleton
class JetpackTunnelWPAPINetwork @Inject constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent), WPAPINetwork {
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
    ): WPAPIResponse<T> {
        return jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = path,
            params = params,
            clazz = clazz,
            enableCaching = enableCaching,
            cacheTimeToLive = cacheTimeToLive,
            forced = forced,
            retryPolicy = DefaultRetryPolicy(
                requestTimeout,
                retries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        ).toWPAPIResponse()
    }

    override suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> {
        return jetpackTunnelGsonRequestBuilder.syncPostRequest(
            restClient = this,
            site = site,
            url = path,
            clazz = clazz,
            body = body
        ).toWPAPIResponse()
    }

    override suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>,
        params: Map<String, String>
    ): WPAPIResponse<T> {
        return jetpackTunnelGsonRequestBuilder.syncPutRequest(
            restClient = this,
            site = site,
            url = path,
            clazz = clazz,
            body = body,
            params = params
        ).toWPAPIResponse()
    }

    override suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>
    ): WPAPIResponse<T> {
        return jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            site = site,
            url = path,
            clazz = clazz,
            params = params
        ).toWPAPIResponse()
    }

    private fun <T> JetpackResponse<T>.toWPAPIResponse(): WPAPIResponse<T> {
        return when (this) {
            is JetpackSuccess -> WPAPIResponse.Success(data, headers)
            is JetpackError -> WPAPIResponse.Error(
                WPAPINetworkError(
                    error,
                    errorCode = error.apiError,
                    errorData = error.errorData
                )
            )
        }
    }
}
