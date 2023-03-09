package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.QueryParameter
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.util.JSONObjectProvider
import okhttp3.HttpUrl
import okhttp3.Request
import okio.Buffer
import javax.inject.Inject

private const val WPCOM_HOST = "public-api.wordpress.com"
private const val JETPACK_TUNNEL_REGEX = "/rest/v1.1/jetpack-blogs/\\d+/rest-api"

internal class EndpointProcessor @Inject constructor(
    private val endpointDao: EndpointDao,
    private val jsonObjectProvider: JSONObjectProvider
) {
    fun fakeRequestIfNeeded(request: Request): Response? {
        // TODO match against method and query parameters too
        val endpointData = when {
            request.url.host == WPCOM_HOST -> request.extractDataFromWPComEndpoint()
            request.url.encodedPath.startsWith("/wp-json") -> request.extractDataFromWPApiEndpoint()
            else -> request.extractDataFromCustomEndpoint()
        }

        return with(endpointData) {
            endpointDao.queryEndpoint(apiType, request.httpMethod, path.trimEnd('/'), body.orEmpty())
        }.filter {
            request.url.checkQueryParameters(it.request.queryParameters)
        }.firstOrNull()?.response
    }

    private fun Request.extractDataFromWPComEndpoint(): EndpointData {
        val originalBody = readBody()
        return if (url.encodedPath.trimEnd('/').matches(Regex(JETPACK_TUNNEL_REGEX))) {
            val (path, body) = if (method == "GET") {
                Pair(
                    url.queryParameter("path")!!.substringBefore("&"),
                    null
                )
            } else {
                val jsonObject = jsonObjectProvider.parseString(originalBody)
                Pair(
                    jsonObject.getString("path").substringBefore("&"),
                    jsonObject.optString("body")
                )
            }

            EndpointData(
                apiType = ApiType.WPApi,
                path = path,
                body = body
            )
        } else {
            EndpointData(
                apiType = ApiType.WPCom,
                path = url.encodedPath.substringAfter("/rest"),
                body = originalBody
            )
        }
    }

    private fun Request.extractDataFromWPApiEndpoint(): EndpointData {
        return EndpointData(
            apiType = ApiType.WPApi,
            path = url.encodedPath.substringAfter("/wp-json"),
            body = readBody()
        )
    }

    private fun Request.extractDataFromCustomEndpoint(): EndpointData {
        return EndpointData(
            apiType = ApiType.Custom(host = url.host),
            path = url.encodedPath,
            body = readBody()
        )
    }

    private fun HttpUrl.checkQueryParameters(queryParameters: List<QueryParameter>): Boolean {
        if (queryParameters.isEmpty()) return true

        return queryParameters.all { queryParameter ->
            val regex = Regex(queryParameter.value.replace("%", ".*"))
            queryParameter(queryParameter.name)?.matches(regex) == true
        }
    }

    private fun Request.readBody(): String {
        val requestBody = body
        return if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            buffer.readUtf8()
        } else ""
    }

    private val Request.httpMethod
        get() = HttpMethod.valueOf(this.method.uppercase())

    private data class EndpointData(
        val apiType: ApiType,
        val path: String,
        val body: String?
    )
}
