package org.wordpress.android.fluxc.jetpacktunnel

import android.net.Uri
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class JetpackTunnelGsonRequestTest {
    companion object {
        private const val DUMMY_SITE_ID = 567L
    }

    private val gson by lazy { Gson() }

    @Test
    fun testCreateGetRequest() {
        val url = "/"
        val params = mapOf("context" to "view")

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, DUMMY_SITE_ID, params,
                Any::class.java,
                { _: Any?, _: Any? -> },
            { _ -> },
                {}
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(DUMMY_SITE_ID).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(3, parsedUri.queryParameterNames.size)
        assertEquals("/&_method=get", parsedUri.getQueryParameter("path"))
        assertEquals("{\"context\":\"view\"}", parsedUri.getQueryParameter("query"))
        assertEquals("true", parsedUri.getQueryParameter("json"))

        // The wrapped GET request should have no body
        val bodyField = request!!::class.java.superclass.getDeclaredField("mBody")
        bodyField.isAccessible = true
        assertNull(bodyField.get(request))
    }

    @Test
    fun testCreatePostRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = JetpackTunnelGsonRequest.buildPostRequest(url, DUMMY_SITE_ID, requestBody,
                Any::class.java,
                { _: Any?, _: Any? -> },
            { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(DUMMY_SITE_ID).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=post", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun testCreatePutRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = JetpackTunnelGsonRequest.buildPutRequest(url, DUMMY_SITE_ID, requestBody,
                Any::class.java,
                { _: Any?, _: Any? -> },
            { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(DUMMY_SITE_ID).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=put", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun `given params, when creating a put request, then they are appended to the path with an ampersand`() {
        val url = "/wc/v3/orders/7997"

        val request = JetpackTunnelGsonRequest.buildPutRequest(url, DUMMY_SITE_ID, mapOf(),
            Any::class.java,
            { _: Any?, _: Any? -> },
            { _ -> },
            params = mapOf("currency" to "EUR")
        )

        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals("/wc/v3/orders/7997&currency=EUR&_method=put", generatedBody["path"])
    }

    @Test
    fun `given params with reserved characters, when creating a put request, then they are encoded`() {
        val url = "/wc/v3/orders/7997"

        val request = JetpackTunnelGsonRequest.buildPutRequest(url, DUMMY_SITE_ID, mapOf(),
            Any::class.java,
            { _: Any?, _: Any? -> },
            { _ -> },
            params = mapOf("search" to "a b&c")
        )

        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals("/wc/v3/orders/7997&search=a%20b%26c&_method=put", generatedBody["path"])
    }

    @Test
    fun testCreatePatchRequest() {
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = JetpackTunnelGsonRequest.buildPatchRequest(url, DUMMY_SITE_ID, requestBody,
                Any::class.java,
                { _: Any?, _: Any? -> },
            { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(DUMMY_SITE_ID).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("/wp/v2/settings/&_method=patch", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
        assertEquals("{\"title\":\"New Title\",\"description\":\"New Description\"}", generatedBody["body"])
    }

    @Test
    fun testCreateDeleteRequest() {
        val url = "/wp/v2/posts/6"
        val params = mapOf("force" to "true")

        val request = JetpackTunnelGsonRequest.buildDeleteRequest(url, DUMMY_SITE_ID, params,
                Any::class.java,
                { _: Any?, _: Any? -> },
            { _ -> }
        )

        // Verify that the request was built and wrapped as expected
        assertEquals(WPCOMREST.jetpack_blogs.site(DUMMY_SITE_ID).rest_api.urlV1_1, UrlUtils.removeQuery(request?.url))
        val parsedUri = Uri.parse(request?.url)
        assertEquals(0, parsedUri.queryParameterNames.size)
        val body = String(request?.body!!)
        val generatedBody = gson.fromJson(body, HashMap<String, String>()::class.java)
        assertEquals(3, generatedBody.size)
        assertEquals("{\"force\":\"true\"}", generatedBody["body"])
        assertEquals("/wp/v2/posts/6&_method=delete", generatedBody["path"])
        assertEquals("true", generatedBody["json"])
    }

    @Test
    fun `given failed direct tunnel request, when error is delivered, then API warning logs and listener receives error`() {
        val receivedErrors = mutableListOf<WPComGsonNetworkError>()
        val request = buildRequest("GET", "/wc/v3/orders", receivedErrors)
        val volleyError = buildVolleyError()

        mockStatic(AppLog::class.java).use { appLog ->
            request?.deliverError(volleyError)

            appLog.verify {
                AppLog.w(
                    AppLog.T.API,
                    "Jetpack Tunnel raw_body error: method=GET, path=/wc/v3/orders, " +
                        "transport_status=502, proxy_status=500, error_code=no_response_body, " +
                        "error_message=Remote site returned non-JSON response, " +
                        "raw_body_truncated=false, raw_body_snippet=<html>Fatal error</html>"
                )
            }
        }
        assertThat(receivedErrors).hasSize(1)
        assertEquals("no_response_body", receivedErrors.single().apiError)
        assertEquals("<html>Fatal error</html>", receivedErrors.single().errorData?.optString("raw_body"))
    }

    @Test
    fun `given failed direct tunnel requests, when errors are delivered, then each factory logs method and path`() {
        val methods = listOf("GET", "POST", "PATCH", "PUT", "DELETE")
        methods.forEach { method ->
            val path = "/wc/v3/orders/${method.lowercase()}"
            val receivedErrors = mutableListOf<WPComGsonNetworkError>()
            val request = buildRequest(method, path, receivedErrors)

            mockStatic(AppLog::class.java).use { appLog ->
                request.deliverError(buildVolleyError())

                appLog.verify {
                    AppLog.w(
                        AppLog.T.API,
                        "Jetpack Tunnel raw_body error: method=$method, path=$path, " +
                            "transport_status=502, proxy_status=500, error_code=no_response_body, " +
                            "error_message=Remote site returned non-JSON response, " +
                            "raw_body_truncated=false, raw_body_snippet=<html>Fatal error</html>"
                    )
                }
            }
            assertThat(receivedErrors).hasSize(1)
        }
    }

    private fun buildRequest(
        method: String,
        path: String,
        receivedErrors: MutableList<WPComGsonNetworkError>
    ): WPComGsonRequest<*> {
        val body = mapOf<String, Any>("name" to "test")
        val listener = { _: Any?, _: Any? -> }
        val errorListener = { error: WPComGsonNetworkError -> receivedErrors.add(error); Unit }
        return when (method) {
            "GET" -> JetpackTunnelGsonRequest.buildGetRequest(
                path,
                DUMMY_SITE_ID,
                emptyMap(),
                Any::class.java,
                listener,
                errorListener,
                null
            )
            "POST" -> JetpackTunnelGsonRequest.buildPostRequest(
                path,
                DUMMY_SITE_ID,
                body,
                Any::class.java,
                listener,
                errorListener
            )
            "PATCH" -> JetpackTunnelGsonRequest.buildPatchRequest(
                path,
                DUMMY_SITE_ID,
                body,
                Any::class.java,
                listener,
                errorListener
            )
            "PUT" -> JetpackTunnelGsonRequest.buildPutRequest(
                path,
                DUMMY_SITE_ID,
                body,
                Any::class.java,
                listener,
                errorListener
            )
            "DELETE" -> JetpackTunnelGsonRequest.buildDeleteRequest(
                path,
                DUMMY_SITE_ID,
                emptyMap(),
                Any::class.java,
                listener,
                errorListener
            )
            else -> error("Unsupported method $method")
        } ?: error("Expected request for $method")
    }

    private fun buildVolleyError(): VolleyError {
        val responseJson = """
            {
              "error": "no_response_body",
              "message": "Remote site returned non-JSON response",
              "data": {
                "status": 500,
                "raw_body": "<html>Fatal error</html>"
              }
            }
        """.trimIndent()
        return VolleyError(NetworkResponse(502, responseJson.toByteArray(), emptyMap(), true))
    }
}
