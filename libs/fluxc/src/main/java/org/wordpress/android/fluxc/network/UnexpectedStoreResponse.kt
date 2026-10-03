package org.wordpress.android.fluxc.network

import android.os.Parcelable
import com.android.volley.NetworkResponse
import com.android.volley.toolbox.HttpHeaderParser
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.nio.charset.Charset

@Parcelize
data class UnexpectedStoreResponse(
    val kind: UnexpectedStoreResponseKind,
    val statusCode: Int,
    val contentType: String?,
    val requestType: String,
    val excerpt: String?
) : Parcelable {
    companion object {
        @JvmStatic
        fun from(statusCode: Int, contentType: String?, body: String, requestType: String): UnexpectedStoreResponse? {
            val kind = UnexpectedStoreResponseClassifier.classify(statusCode, contentType, body) ?: return null
            return of(kind, statusCode, contentType, body, requestType)
        }

        @JvmStatic
        fun from(response: NetworkResponse, method: Int, url: String): UnexpectedStoreResponse? = from(
            statusCode = response.statusCode,
            contentType = response.contentType(),
            body = response.body(),
            requestType = requestType(method, url)
        )

        fun of(
            kind: UnexpectedStoreResponseKind,
            statusCode: Int,
            contentType: String?,
            body: String,
            requestType: String
        ) = UnexpectedStoreResponse(
            kind = kind,
            statusCode = statusCode,
            contentType = contentType,
            requestType = requestType,
            excerpt = UnexpectedStoreResponseExcerpt.from(body)
        )

        fun of(kind: UnexpectedStoreResponseKind, response: NetworkResponse, method: Int, url: String) = of(
            kind = kind,
            statusCode = response.statusCode,
            contentType = response.contentType(),
            body = response.body(),
            requestType = requestType(method, url)
        )

        @JvmStatic
        fun requestType(method: Int, url: String): String = "${method.toMethodName()} ${url.toRestRoute()}"

        private fun NetworkResponse.contentType(): String? =
            headers?.entries?.firstOrNull { it.key.equals(CONTENT_TYPE, ignoreCase = true) }?.value

        private fun NetworkResponse.body(): String = data?.let { String(it, charset()) }.orEmpty()

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
