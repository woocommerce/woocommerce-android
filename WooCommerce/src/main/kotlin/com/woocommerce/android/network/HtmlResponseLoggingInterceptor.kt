package com.woocommerce.android.network

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.WooLog
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response

/**
 * Logs responses that return `text/html` when the app expected JSON, to help support diagnose
 * environment-specific failures (firewall/CDN blocks, login redirects, maintenance pages, etc.).
 *
 * Disabled by default; enabled temporarily via the "Advanced error logging" toggle in Help & Support.
 */
class HtmlResponseLoggingInterceptor(
    private val appPrefsWrapper: AppPrefsWrapper
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (appPrefsWrapper.isAdvancedHtmlErrorLoggingEnabled()) {
            buildLogMessage(request, response)?.let { WooLog.w(WooLog.T.UTILS, it) }
        }

        return response
    }

    companion object {
        const val MAX_BODY_PREVIEW_BYTES = 1024L

        private val SENSITIVE_PARAMS = setOf("access_token", "token", "auth", "password", "secret")

        /**
         * Builds a log message when [response] is an HTML response, or returns `null` otherwise.
         * Side-effect free so it can be unit tested without touching the logger.
         */
        internal fun buildLogMessage(request: Request, response: Response): String? {
            val contentType = response.header("Content-Type") ?: return null
            if (!contentType.contains("text/html", ignoreCase = true)) return null

            val bodyPreview = response.peekBody(MAX_BODY_PREVIEW_BYTES).string()
            return buildString {
                appendLine("[HTML Response Detected]")
                appendLine("  Endpoint: ${redactSensitiveParams(request.url)}")
                appendLine("  Status: ${response.code}")
                appendLine("  Content-Type: $contentType")
                appendLine("  Body preview: $bodyPreview")
                append("  Redirect: ${response.header("Location") ?: "(none)"}")
            }
        }

        private fun redactSensitiveParams(url: HttpUrl): String {
            if (url.queryParameterNames.none { it.lowercase() in SENSITIVE_PARAMS }) {
                return url.toString()
            }
            val builder = url.newBuilder()
            url.queryParameterNames
                .filter { it.lowercase() in SENSITIVE_PARAMS }
                .forEach { name ->
                    builder.removeAllQueryParameters(name)
                    builder.addQueryParameter(name, "[REDACTED]")
                }
            return builder.build().toString()
        }
    }
}
