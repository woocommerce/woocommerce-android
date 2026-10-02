package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.toolbox.HttpHeaderParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.nio.charset.Charset

data class UnexpectedStoreResponse(
    val kind: UnexpectedStoreResponseKind,
    val statusCode: Int,
    val contentType: String?,
    val requestType: String,
    val excerpt: String?
) {
    companion object {
        @JvmStatic
        fun from(statusCode: Int, contentType: String?, body: String, requestType: String): UnexpectedStoreResponse? {
            val kind = UnexpectedStoreResponseClassifier.classify(statusCode, contentType, body) ?: return null
            return UnexpectedStoreResponse(
                kind = kind,
                statusCode = statusCode,
                contentType = contentType,
                requestType = requestType,
                excerpt = UnexpectedStoreResponseExcerpt.from(body)
            )
        }

        @JvmStatic
        fun from(response: NetworkResponse, method: Int, url: String): UnexpectedStoreResponse? = from(
            statusCode = response.statusCode,
            contentType = response.headers?.entries?.firstOrNull { it.key.equals(CONTENT_TYPE, ignoreCase = true) }
                ?.value,
            body = response.data?.let { String(it, response.charset()) }.orEmpty(),
            requestType = "${method.toMethodName()} ${url.toRestRoute()}"
        )

        private fun NetworkResponse.charset(): Charset = runCatching {
            Charset.forName(HttpHeaderParser.parseCharset(headers, Charsets.UTF_8.name()))
        }.getOrDefault(Charsets.UTF_8)

        private fun Int.toMethodName(): String =
            HttpMethod.entries.firstOrNull { it.toVolleyMethod() == this }?.name ?: UNKNOWN_METHOD

        private fun String.toRestRoute(): String {
            val url = toHttpUrlOrNull() ?: return ""
            url.queryParameter(REST_ROUTE_PARAMETER)?.let { return it }
            val path = url.encodedPath
            return path.substringAfter(REST_PATH_PREFIX, missingDelimiterValue = path).ifEmpty { "/" }
        }

        private const val CONTENT_TYPE = "Content-Type"
        private const val UNKNOWN_METHOD = "UNKNOWN"
        private const val REST_ROUTE_PARAMETER = "rest_route"
        private const val REST_PATH_PREFIX = "/wp-json"
    }
}
