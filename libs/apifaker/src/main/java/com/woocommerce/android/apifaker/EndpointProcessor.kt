package com.woocommerce.android.apifaker

import android.util.Log
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
        val endpointData = when {
            request.url.host == WPCOM_HOST -> request.extractDataFromWPComEndpoint()
            request.url.encodedPath.startsWith("/wp-json") -> request.extractDataFromWPApiEndpoint()
            else -> request.extractDataFromCustomEndpoint()
        }

        return with(endpointData) {
            endpointDao.queryEndpoint(apiType, httpMethod, path.trimEnd('/'), body.orEmpty())
        }.filter {
            request.url.checkQueryParameters(it.request.queryParameters)
        }.also {
            if (it.size > 1) {
                Log.w(
                    LOG_TAG,
                    "More than one endpoint matched the request: $request, " +
                        "the endpoints matched are\n$it\n" +
                        "The first one will be used."
                )
            }
        }.firstOrNull()?.response?.let {
            it.copy(body = it.body?.wrapBodyIfNecessary(request.url))
        }
    }

    private fun Request.extractDataFromWPComEndpoint(): EndpointData {
        val originalBody = readBody()
        return if (url.isJetpackTunnelRequest) {
            val (path, method, body) = if (method == "GET") {
                Triple(
                    url.queryParameter("path")!!.substringBefore("&"),
                    url.queryParameter("_method") ?: "GET",
                    null
                )
            } else {
                val jsonObject = jsonObjectProvider.parseString(originalBody)
                val pathParts = jsonObject.getString("path").split("&")
                val path = pathParts[0]
                val method = pathParts.firstOrNull { it.startsWith("_method") }?.split("=")?.getOrNull(1) ?: "POST"
                Triple(
                    path,
                    method,
                    jsonObject.optString("body")
                )
            }

            EndpointData(
                apiType = ApiType.WPApi,
                httpMethod = HttpMethod.valueOf(method.uppercase()),
                path = path,
                body = body
            )
        } else {
            EndpointData(
                apiType = ApiType.WPCom,
                httpMethod = httpMethod,
                path = url.encodedPath.substringAfter("/rest"),
                body = originalBody
            )
        }
    }

    private fun Request.extractDataFromWPApiEndpoint(): EndpointData {
        return EndpointData(
            apiType = ApiType.WPApi,
            httpMethod = httpMethod,
            path = url.encodedPath.substringAfter("/wp-json"),
            body = readBody()
        )
    }

    private fun Request.extractDataFromCustomEndpoint(): EndpointData {
        return EndpointData(
            apiType = ApiType.Custom(host = url.host),
            httpMethod = httpMethod,
            path = url.encodedPath,
            body = readBody()
        )
    }

    private fun HttpUrl.checkQueryParameters(mockedQueryParameters: List<QueryParameter>): Boolean {
        if (mockedQueryParameters.isEmpty()) return true

        val requestQueryParameters = if (isJetpackTunnelRequest) {
            queryParameter("query")?.let {
                val json = jsonObjectProvider.parseString(it)
                json.keys().asSequence().map { key ->
                    key to json.getString(key)
                }.toMap()
            } ?: emptyMap()
        } else {
            queryParameterNames.associateWith { queryParameter(it) }
        }

        return mockedQueryParameters.all { queryParameter ->
            requestQueryParameters[queryParameter.name] == queryParameter.value
        }
    }

    private fun Request.readBody(): String {
        val requestBody = body
        return if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            buffer.readUtf8()
        } else {
            ""
        }
    }

    private fun String.wrapBodyIfNecessary(url: HttpUrl): String {
        return if (url.isJetpackTunnelRequest &&
            !startsWith("{\"data\":")
        ) {
            "{\"data\": $this}"
        } else {
            this
        }
    }

    private val Request.httpMethod
        get() = HttpMethod.valueOf(this.method.uppercase())

    private val HttpUrl.isJetpackTunnelRequest
        get() = host == WPCOM_HOST && encodedPath.trimEnd('/').matches(Regex(JETPACK_TUNNEL_REGEX))

    private data class EndpointData(
        val apiType: ApiType,
        val path: String,
        val httpMethod: HttpMethod,
        val body: String?
    )
}
