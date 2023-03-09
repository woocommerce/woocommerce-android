package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.util.JSONObjectProvider
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EndpointProcessorTest {
    private val endpointDaoMock = mock<EndpointDao>()
    private val jsonObjectProvider = mock<JSONObjectProvider>()
    private val endpointProcessor = EndpointProcessor(
        endpointDao = endpointDaoMock,
        jsonObjectProvider = jsonObjectProvider
    )

    @Test
    fun `when processing a GET WPCom endpoint, then extract data correctly`() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://public-api.wordpress.com/rest/v1.1/me?param=value")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPCom,
            httpMethod = HttpMethod.GET,
            path = "/v1.1/me",
            body = ""
        )
    }

    @Test
    fun `when processing a POST WPCom endpoint, then extract data correctly`() {
        val body = "Test Body"
        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url("https://public-api.wordpress.com/rest/v1.1/me?param=value")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPCom,
            httpMethod = HttpMethod.POST,
            path = "/v1.1/me",
            body = body
        )
    }

    @Test
    fun `when processing a GET Jetpack Tunnel endpoint, then extract data correctly`() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api/?path=/wc/v3/products&_method=get")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPApi,
            httpMethod = HttpMethod.GET,
            path = "/wc/v3/products",
            body = ""
        )
    }

    @Test
    fun `when processing a POST Jetpack Tunnel endpoint, then extract data correctly`() {
        val body = """
            "path": "/wc/v3/products",
            "body": "test body"
        """.trimIndent()

        val jsonObject = mock<JSONObject> {
            on { getString("path") } doReturn "/wc/v3/products"
            on { optString("body") } doReturn "test body"
        }
        whenever(jsonObjectProvider.parseString(body)).thenReturn(jsonObject)

        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url("https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPApi,
            httpMethod = HttpMethod.POST,
            path = "/wc/v3/products",
            body = "test body"
        )
    }

    @Test
    fun `when processing a GET WPApi endpoint, then extract data correctly`() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://test-site.com/wp-json/wc/v3/products?param=value")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPApi,
            httpMethod = HttpMethod.GET,
            path = "/wc/v3/products",
            body = ""
        )
    }

    @Test
    fun `when processing a POST WPApi endpoint, then extract data correctly`() {
        val body = "Test Body"
        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url("https://test-site.com/wp-json/wc/v3/products")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPApi,
            httpMethod = HttpMethod.POST,
            path = "/wc/v3/products",
            body = body
        )
    }

    @Test
    fun `when processing a GET Custom endpoint, then extract data correctly`() {
        val request = Request.Builder()
            .method("GET", null)
            .url("https://test-site.com/an/endpoint?param=value")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.Custom("test-site.com"),
            httpMethod = HttpMethod.GET,
            path = "/an/endpoint",
            body = ""
        )
    }

    @Test
    fun `when processing a POST Custom endpoint, then extract data correctly`() {
        val body = "Test Body"
        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url("https://test-site.com/an/endpoint")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.Custom("test-site.com"),
            httpMethod = HttpMethod.POST,
            path = "/an/endpoint",
            body = body
        )
    }
}
