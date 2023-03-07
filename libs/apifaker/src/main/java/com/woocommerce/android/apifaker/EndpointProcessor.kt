package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.EndpointType
import com.woocommerce.android.apifaker.models.FakeResponse
import com.woocommerce.android.apifaker.util.JSONObjectProvider
import okhttp3.Request
import okio.Buffer
import javax.inject.Inject

private const val WPCOM_HOST = "public-api.wordpress.com"
private const val JETPACK_TUNNEL_REGEX = "/rest/v1.1/jetpack-blogs/\\d+/rest-api"

internal class EndpointProcessor @Inject constructor(
    private val endpointDao: EndpointDao,
    private val jsonObjectProvider: JSONObjectProvider
) {
    fun fakeRequestIfNeeded(request: Request): FakeResponse? {
        // TODO match against method and query parameters too
        val endpointData = when {
            request.url.host == WPCOM_HOST -> request.extractDataFromWPComEndpoint()
            request.url.encodedPath.startsWith("/wp-json") -> request.extractDataFromWPApiEndpoint()
            else -> request.extractDataFromCustomEndpoint()
        }

        return with(endpointData) {
            endpointDao.queryEndpoint(endpointType, path, body.orEmpty())
        }?.response
    }

    private fun Request.extractDataFromWPComEndpoint(): EndpointData {
        val originalBody = readBody()
        return if (url.encodedPath.trimEnd('/').matches(Regex(JETPACK_TUNNEL_REGEX))) {
            val (path, body) = if (method == "GET") {
                Pair(
                    url.queryParameter("path")!!,
                    null
                )
            } else {
                val jsonObject = jsonObjectProvider.parseString(originalBody)
                Pair(
                    jsonObject.getString("path"),
                    jsonObject.optString("body")
                )
            }

            EndpointData(
                endpointType = EndpointType.WPApi,
                path = path,
                body = body
            )
        } else {
            EndpointData(
                endpointType = EndpointType.WPCom,
                path = url.encodedPath.substringAfter("/rest"),
                body = originalBody
            )
        }
    }

    private fun Request.extractDataFromWPApiEndpoint(): EndpointData {
        return EndpointData(
            endpointType = EndpointType.WPApi,
            path = url.encodedPath.substringAfter("/wp-json"),
            body = readBody()
        )
    }

    private fun Request.extractDataFromCustomEndpoint(): EndpointData {
        return EndpointData(
            endpointType = EndpointType.Custom(host = url.host),
            path = url.encodedPath,
            body = readBody()
        )
    }

    private fun Request.readBody(): String {
        val requestBody = body
        return if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            buffer.readUtf8()
        } else ""
    }

    private data class EndpointData(
        val endpointType: EndpointType,
        val path: String,
        val body: String?
    )
}
