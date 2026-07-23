package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject
import javax.inject.Named

class CookieNonceWPAPINetwork @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val cookieNonceAuthenticator: CookieNonceAuthenticator,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent), WPAPINetwork {
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
        return cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.buildUrl(path),
                params = params,
                body = emptyMap(),
                clazz = clazz,
                enableCaching = enableCaching,
                cacheTimeToLive = cacheTimeToLive,
                nonce = nonce.value,
            )
        }
    }

    override suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ): WPAPIResponse<T> {
        return cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncPostRequest(
                restClient = this,
                url = site.buildUrl(path),
                body = body,
                clazz = clazz,
                nonce = nonce.value
            )
        }
    }

    override suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>,
        params: Map<String, String>
    ): WPAPIResponse<T> {
        return cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncPutRequest(
                restClient = this,
                url = site.buildUrl(path),
                body = body,
                clazz = clazz,
                nonce = nonce.value,
                params = params
            )
        }
    }

    override suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>
    ): WPAPIResponse<T> {
        return cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            wpApiGsonRequestBuilder.syncDeleteRequest(
                restClient = this,
                url = site.buildUrl(path),
                body = emptyMap(),
                clazz = clazz,
                nonce = nonce.value
            )
        }
    }

    private fun SiteModel.buildUrl(path: String): String {
        return wpApiRestUrl.ifEmpty { WPAPIDiscoveryUtils.buildDefaultRESTBaseUrl(url) }
            .slashJoin(path)
    }
}
