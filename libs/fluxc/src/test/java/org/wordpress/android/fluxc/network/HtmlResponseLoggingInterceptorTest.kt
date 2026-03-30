package org.wordpress.android.fluxc.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class HtmlResponseLoggingInterceptorTest {
    private var loggingEnabled = false
    private val detectedResponses = mutableListOf<DetectedResponse>()

    private val config = object : HtmlResponseLoggingConfig {
        override val isEnabled: Boolean get() = loggingEnabled

        override fun onHtmlResponseDetected(
            endpoint: String,
            statusCode: Int,
            contentType: String?,
            bodyPreview: String,
            redirectTarget: String?
        ) {
            detectedResponses.add(
                DetectedResponse(endpoint, statusCode, contentType, bodyPreview, redirectTarget)
            )
        }
    }

    private val interceptor = HtmlResponseLoggingInterceptor(config)

    @Test
    fun `when logging is disabled and response is HTML, then callback is not invoked`() {
        loggingEnabled = false
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 503,
            responseContentType = "text/html",
            responseBody = "<html><body>Service Unavailable</body></html>"
        )

        interceptor.intercept(chain)

        assertTrue(detectedResponses.isEmpty())
    }

    @Test
    fun `when logging is enabled and response is HTML, then callback is invoked with correct data`() {
        loggingEnabled = true
        val htmlBody = "<html><body>503 Service Temporarily Unavailable</body></html>"
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 503,
            responseContentType = "text/html; charset=UTF-8",
            responseBody = htmlBody
        )

        interceptor.intercept(chain)

        assertEquals(1, detectedResponses.size)
        val detected = detectedResponses.first()
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/123/posts", detected.endpoint)
        assertEquals(503, detected.statusCode)
        assertEquals("text/html; charset=UTF-8", detected.contentType)
        assertEquals(htmlBody, detected.bodyPreview)
    }

    @Test
    fun `when logging is enabled and response is JSON, then callback is not invoked`() {
        loggingEnabled = true
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 200,
            responseContentType = "application/json",
            responseBody = """{"posts":[]}"""
        )

        interceptor.intercept(chain)

        assertTrue(detectedResponses.isEmpty())
    }

    @Test
    fun `when response URL contains auth tokens, then they are redacted from logged endpoint`() {
        loggingEnabled = true
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts?access_token=secret123&other=keep",
            responseCode = 403,
            responseContentType = "text/html",
            responseBody = "<html>Forbidden</html>"
        )

        interceptor.intercept(chain)

        assertEquals(1, detectedResponses.size)
        val endpoint = detectedResponses.first().endpoint
        assertTrue(endpoint.contains("access_token=%5BREDACTED%5D"))
        assertTrue(endpoint.contains("other=keep"))
    }

    @Test
    fun `when response body exceeds max size, then body preview is truncated`() {
        loggingEnabled = true
        val largeBody = "x".repeat(2048)
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 503,
            responseContentType = "text/html",
            responseBody = largeBody
        )

        interceptor.intercept(chain)

        assertEquals(1, detectedResponses.size)
        assertTrue(
            detectedResponses.first().bodyPreview.length <= HtmlResponseLoggingInterceptor.MAX_BODY_PREVIEW_BYTES
        )
    }

    @Test
    fun `when response has Location header, then redirect target is logged`() {
        loggingEnabled = true
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 302,
            responseContentType = "text/html",
            responseBody = "<html>Redirecting...</html>",
            locationHeader = "https://login.wordpress.com/wp-login.php"
        )

        interceptor.intercept(chain)

        assertEquals(1, detectedResponses.size)
        assertEquals("https://login.wordpress.com/wp-login.php", detectedResponses.first().redirectTarget)
    }

    @Test
    fun `when response has no Location header, then redirect target is null`() {
        loggingEnabled = true
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 503,
            responseContentType = "text/html",
            responseBody = "<html>Error</html>"
        )

        interceptor.intercept(chain)

        assertEquals(1, detectedResponses.size)
        assertEquals(null, detectedResponses.first().redirectTarget)
    }

    @Test
    fun `when response has no Content-Type header, then callback is not invoked`() {
        loggingEnabled = true
        val chain = createChain(
            requestUrl = "https://public-api.wordpress.com/rest/v1.1/sites/123/posts",
            responseCode = 200,
            responseContentType = null,
            responseBody = "some body"
        )

        interceptor.intercept(chain)

        assertTrue(detectedResponses.isEmpty())
    }

    private fun createChain(
        requestUrl: String,
        responseCode: Int,
        responseContentType: String?,
        responseBody: String,
        locationHeader: String? = null
    ): Interceptor.Chain {
        val request = Request.Builder().url(requestUrl).build()
        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseCode)
            .message("OK")

        if (responseContentType != null) {
            responseBuilder.header("Content-Type", responseContentType)
            responseBuilder.body(responseBody.toResponseBody(responseContentType.toMediaType()))
        } else {
            responseBuilder.body(responseBody.toResponseBody(null))
        }

        if (locationHeader != null) {
            responseBuilder.header("Location", locationHeader)
        }

        val response = responseBuilder.build()
        val chain = mock(Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(response)
        return chain
    }

    private data class DetectedResponse(
        val endpoint: String,
        val statusCode: Int,
        val contentType: String?,
        val bodyPreview: String,
        val redirectTarget: String?
    )
}
