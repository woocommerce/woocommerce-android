package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import javax.inject.Inject
import kotlin.coroutines.resume

class WPAPIEncodedBodyRequestBuilder @Inject constructor(
    private val httpsUrlNormalizer: HttpsUrlNormalizer,
) {
    suspend fun syncGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<String>> { cont ->
        callMethod(Method.GET, url, params, body, cont, enableCaching, cacheTimeToLive, nonce, restClient)
    }

    suspend fun syncPostRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<String>> { cont ->
        callMethod(Method.POST, url, params, body, cont, enableCaching, cacheTimeToLive, nonce, restClient)
    }

    @Suppress("LongParameterList")
    private fun callMethod(
        method: Int,
        url: String,
        params: Map<String, String>,
        body: Map<String, String>,
        cont: CancellableContinuation<WPAPIResponse<String>>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        nonce: String?,
        restClient: BaseWPAPIRestClient
    ) {
        val normalizedUrl = httpsUrlNormalizer.normalize(url).normalizedUrl
        val request = WPAPIEncodedBodyRequest(method, normalizedUrl, params, body, { response, headers ->
            cont.resume(Success(response, headers))
        }, { error ->
            cont.resume(Error(error))
        })

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        if (nonce != null) {
            request.addHeader("x-wp-nonce", nonce)
        }

        restClient.add(request)
    }
}
