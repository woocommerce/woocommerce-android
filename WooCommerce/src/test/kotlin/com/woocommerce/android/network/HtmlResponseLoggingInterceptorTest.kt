package com.woocommerce.android.network

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.network.HtmlResponseLoggingInterceptor.Companion.buildLogMessage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class HtmlResponseLoggingInterceptorTest {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val interceptor = HtmlResponseLoggingInterceptor(appPrefsWrapper)

    @Test
    fun `when response is HTML, then message contains endpoint, status, content-type and body`() {
        val htmlBody = "<html><body>503 Service Temporarily Unavailable</body></html>"
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(503, "text/html; charset=UTF-8", htmlBody)
        )

        assertThat(message).isNotNull()
        assertThat(message).contains("https://public-api.wordpress.com/rest/v1.1/sites/123/posts")
        assertThat(message).contains("Method: GET")
        assertThat(message).contains("Status: 503")
        assertThat(message).contains("Content-Type: text/html; charset=UTF-8")
        assertThat(message).contains(htmlBody)
    }

    @Test
    fun `when request is HEAD, then no message is built even for an HTML response`() {
        val message = buildLogMessage(
            request = Request.Builder()
                .url("https://static-grouse-ferret.jurassic.ninja/")
                .head()
                .build(),
            response = response(200, "text/html; charset=UTF-8", "")
        )

        assertThat(message).isNull()
    }

    @Test
    fun `when response is JSON, then no message is built`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(200, "application/json", """{"posts":[]}""")
        )

        assertThat(message).isNull()
    }

    @Test
    fun `when response has no Content-Type, then no message is built`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(200, contentType = null, body = "some body")
        )

        assertThat(message).isNull()
    }

    @Test
    fun `when URL contains auth tokens, then they are redacted from the logged endpoint`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123?access_token=secret123&other=keep"),
            response = response(403, "text/html", "<html>Forbidden</html>")
        )

        assertThat(message).contains("access_token=%5BREDACTED%5D")
        assertThat(message).contains("other=keep")
        assertThat(message).doesNotContain("secret123")
    }

    @Test
    fun `when response body exceeds max size, then body preview is truncated`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(503, "text/html", "x".repeat(2048))
        )

        val bodyPreview = message!!.substringAfter("Body preview: ").substringBefore("\n")
        assertThat(bodyPreview.length).isLessThanOrEqualTo(
            HtmlResponseLoggingInterceptor.MAX_BODY_PREVIEW_BYTES.toInt()
        )
    }

    @Test
    fun `when response has Location header, then redirect target is included`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(
                code = 302,
                contentType = "text/html",
                body = "<html>Redirecting...</html>",
                locationHeader = "https://login.wordpress.com/wp-login.php"
            )
        )

        assertThat(message).contains("Redirect: https://login.wordpress.com/wp-login.php")
    }

    @Test
    fun `when response has no Location header, then redirect is reported as none`() {
        val message = buildLogMessage(
            request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts"),
            response = response(503, "text/html", "<html>Error</html>")
        )

        assertThat(message).contains("Redirect: (none)")
    }

    @Test
    fun `when logging is disabled, then the response is returned unchanged and nothing is logged`() {
        whenever(appPrefsWrapper.isAdvancedHtmlErrorLoggingEnabled()).doReturn(false)
        val request = request("https://public-api.wordpress.com/rest/v1.1/sites/123/posts")
        val response = response(503, "text/html", "<html>Error</html>")
        val chain = chain(request, response)

        val result = interceptor.intercept(chain)

        assertThat(result).isSameAs(response)
        verify(appPrefsWrapper).isAdvancedHtmlErrorLoggingEnabled()
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()

    private fun response(
        code: Int,
        contentType: String?,
        body: String,
        locationHeader: String? = null
    ): Response {
        val builder = Response.Builder()
            .request(Request.Builder().url("https://public-api.wordpress.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")

        if (contentType != null) {
            builder.header("Content-Type", contentType)
            builder.body(body.toResponseBody(contentType.toMediaType()))
        } else {
            builder.body(body.toResponseBody(null))
        }
        locationHeader?.let { builder.header("Location", it) }
        return builder.build()
    }

    private fun chain(request: Request, response: Response): Interceptor.Chain = mock {
        on { request() } doReturn request
        on { proceed(request) } doReturn response
    }
}
