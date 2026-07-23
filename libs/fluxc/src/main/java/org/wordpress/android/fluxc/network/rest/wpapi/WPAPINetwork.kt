package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest

interface WPAPINetwork {
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
    ): WPAPIResponse<T>

    suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T>

    suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ): WPAPIResponse<T>

    suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ): WPAPIResponse<T>
}
