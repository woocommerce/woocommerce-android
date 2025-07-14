package com.woocommerce.android.e2e.helpers

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Singleton

var useMockedAPI: Boolean = true

@Singleton
class MockingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
        val request = chain.request()

        // Redirect all WordPress.com REST API requests to local mock server
        if (useMockedAPI && request.isValidHost) {
            val newUrl = request.url.newBuilder()
                .scheme("http")
                .host("localhost")
                .port(TestBase.wireMockPort)
                .build()
            val newRequest = request.newBuilder()
                .url(newUrl)
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }

    private val Request.isValidHost: Boolean
        get() = url.host == "public-api.wordpress.com" || url.host == "wordpress.com"
}
