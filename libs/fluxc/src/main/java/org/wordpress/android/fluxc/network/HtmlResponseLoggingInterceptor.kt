package org.wordpress.android.fluxc.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class HtmlResponseLoggingInterceptor(
    private val config: HtmlResponseLoggingConfig
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!config.isEnabled) return response

        val contentType = response.header("Content-Type")
        if (contentType != null && contentType.contains("text/html", ignoreCase = true)) {
            val bodyPreview = response.peekBody(MAX_BODY_PREVIEW_BYTES).string()
            val sanitizedUrl = redactSensitiveParams(request.url)
            val redirectTarget = response.header("Location")

            config.onHtmlResponseDetected(
                endpoint = sanitizedUrl,
                statusCode = response.code,
                contentType = contentType,
                bodyPreview = bodyPreview,
                redirectTarget = redirectTarget
            )
        }

        return response
    }

    companion object {
        const val MAX_BODY_PREVIEW_BYTES = 1024L

        private val SENSITIVE_PARAMS = setOf(
            "access_token",
            "token",
            "auth",
            "password",
            "secret"
        )

        fun redactSensitiveParams(url: HttpUrl): String {
            val hasParamsToRedact = url.queryParameterNames.any { it.lowercase() in SENSITIVE_PARAMS }
            if (!hasParamsToRedact) return url.toString()

            val builder = url.newBuilder()
            for (name in url.queryParameterNames) {
                if (name.lowercase() in SENSITIVE_PARAMS) {
                    builder.removeAllQueryParameters(name)
                    builder.addQueryParameter(name, "[REDACTED]")
                }
            }
            return builder.build().toString()
        }
    }
}
