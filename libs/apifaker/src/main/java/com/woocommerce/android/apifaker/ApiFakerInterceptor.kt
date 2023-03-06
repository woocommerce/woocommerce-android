package com.woocommerce.android.apifaker

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

internal class ApiFakerInterceptor @Inject constructor(private val endpointProcessor: EndpointProcessor) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val fakeResponse = endpointProcessor.fakeRequestIfNeeded(request)

        return if (fakeResponse != null) {
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("Fake Response")
                .code(fakeResponse.statusCode)
                // TODO check if it's safe to always use JSON as the content type
                .body(fakeResponse.body?.toResponseBody("application/json".toMediaType()))
                .addHeader("content-type", "application/json")
                .build()
        } else {
            chain.proceed(request)
        }
    }
}
