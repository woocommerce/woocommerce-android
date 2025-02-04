package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.QueryParameter
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.util.JSONObjectProvider
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import okhttp3.Request as OkHttpRequest

class EndpointProcessorTest {
    private val endpointDaoMock = mock<EndpointDao>()
    private val jsonObjectProvider = mock<JSONObjectProvider>()
    private val endpointProcessor = EndpointProcessor(
        endpointDao = endpointDaoMock,
        jsonObjectProvider = jsonObjectProvider
    )

    @Test
    fun `when processing a GET WPCom endpoint, then extract data correctly`() {
        val request = OkHttpRequest.Builder()
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
        val request = OkHttpRequest.Builder()
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
        val request = OkHttpRequest.Builder()
            .method("GET", null)
            .url(
                "https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api/" +
                    "?path=/wc/v3/products&_method=get"
            )
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
            "path": "/wc/v3/products&_method=put",
            "body": "test body"
        """.trimIndent()

        val jsonObject = mock<JSONObject> {
            on { getString("path") } doReturn "/wc/v3/products&_method=put"
            on { optString("body") } doReturn "test body"
        }
        whenever(jsonObjectProvider.parseString(body)).thenReturn(jsonObject)

        val request = OkHttpRequest.Builder()
            .method("POST", body.toRequestBody())
            .url("https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api")
            .build()

        endpointProcessor.fakeRequestIfNeeded(request)

        verify(endpointDaoMock).queryEndpoint(
            type = ApiType.WPApi,
            httpMethod = HttpMethod.PUT,
            path = "/wc/v3/products",
            body = "test body"
        )
    }

    @Test
    fun `when processing a GET WPApi endpoint, then extract data correctly`() {
        val request = OkHttpRequest.Builder()
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
        val request = OkHttpRequest.Builder()
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
        val request = OkHttpRequest.Builder()
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
        val request = OkHttpRequest.Builder()
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

    @Test
    fun `when processing a GET jetpack tunnel endpoint, then wrap body if necessary`() {
        val mockEndpoint = MockedEndpoint(
            request = Request(
                id = 0,
                type = ApiType.WPApi,
                httpMethod = HttpMethod.GET,
                path = "/wc/v3/products",
                body = null
            ),
            response = Response(
                endpointId = 0,
                statusCode = 200,
                body = "{\"key\":\"value\"}"
            )
        )
        whenever(
            endpointDaoMock.queryEndpoint(
                type = mockEndpoint.request.type,
                httpMethod = mockEndpoint.request.httpMethod!!,
                path = mockEndpoint.request.path,
                body = mockEndpoint.request.body.orEmpty()
            )
        ).thenReturn(listOf(mockEndpoint))

        val request = OkHttpRequest.Builder()
            .method(mockEndpoint.request.httpMethod.name, null)
            .url(
                "https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api/" +
                    "?path=${mockEndpoint.request.path}&_method=${mockEndpoint.request.httpMethod.name}"
            )
            .build()

        val response = endpointProcessor.fakeRequestIfNeeded(request)
        assert(response?.body == "{\"data\": {\"key\":\"value\"}}")
    }

    @Test
    fun `when processing a GET jetpack tunnel endpoint with query parameters, then check query parameters`() {
        val mockEndpoint = MockedEndpoint(
            request = Request(
                id = 0,
                type = ApiType.WPApi,
                httpMethod = HttpMethod.GET,
                path = "/wc/v3/products",
                queryParameters = listOf(
                    QueryParameter("param", "value")
                ),
                body = null
            ),
            response = Response(
                endpointId = 0,
                statusCode = 200
            )
        )
        whenever(
            endpointDaoMock.queryEndpoint(
                type = mockEndpoint.request.type,
                httpMethod = mockEndpoint.request.httpMethod!!,
                path = mockEndpoint.request.path,
                body = mockEndpoint.request.body.orEmpty()
            )
        ).thenReturn(listOf(mockEndpoint))
        val jsonObject = mock<JSONObject> {
            on { keys() } doReturn listOf("param").iterator()
            on { getString("param") } doReturn "value"
        }
        whenever(jsonObjectProvider.parseString("{\"param\":\"value\"}")).thenReturn(jsonObject)

        val request = OkHttpRequest.Builder()
            .method(mockEndpoint.request.httpMethod.name, null)
            .url(
                "https://public-api.wordpress.com/rest/v1.1/jetpack-blogs/161477129/rest-api/" +
                    "?path=${mockEndpoint.request.path}&_method=${mockEndpoint.request.httpMethod.name}" +
                    "&query={\"param\":\"value\"}"
            )
            .build()

        val response = endpointProcessor.fakeRequestIfNeeded(request)
        assert(response?.statusCode == 200)
    }

    @Test
    fun `when processing a regular endpoint with query parameters, then check query parameters`() {
        val mockEndpoint = MockedEndpoint(
            request = Request(
                id = 0,
                type = ApiType.WPApi,
                httpMethod = HttpMethod.GET,
                path = "/wc/v3/products",
                queryParameters = listOf(
                    QueryParameter("param", "value")
                ),
                body = null
            ),
            response = Response(
                endpointId = 0,
                statusCode = 200
            )
        )
        whenever(
            endpointDaoMock.queryEndpoint(
                type = mockEndpoint.request.type,
                httpMethod = mockEndpoint.request.httpMethod!!,
                path = mockEndpoint.request.path,
                body = mockEndpoint.request.body.orEmpty()
            )
        ).thenReturn(listOf(mockEndpoint))

        val request = OkHttpRequest.Builder()
            .method(mockEndpoint.request.httpMethod.name, null)
            .url("https://test-site.com/wp-json/wc/v3/products?param=value")
            .build()

        val response = endpointProcessor.fakeRequestIfNeeded(request)
        assert(response?.statusCode == 200)
    }
}
