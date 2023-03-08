package com.woocommerce.android.apifaker

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

internal class ApiFakerInterceptor @Inject constructor(private val endpointProcessor: EndpointProcessor) : Interceptor {
    override fun intercept(chain: Chain): Response {
        Log.d(LOG_TAG, "Intercepting request: ${chain.request().url}")
        val request = chain.request()
        val fakeResponse = try {
            endpointProcessor.fakeRequestIfNeeded(request)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Faking request: ${chain.request()} failed, ignoring")
            null
        }

        return if (fakeResponse != null) {
            Log.d(
                LOG_TAG, "Matched request: ${chain.request().url}:\n" +
                    "Sending Mocked Response: $fakeResponse"
            )
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
