package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.Request.Method
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind

@RunWith(RobolectricTestRunner::class)
class WPAPIGsonRequestTest {
    @Test
    fun `given a 2xx web page, when the error is delivered, then it keeps the parse error type and adds the details`() {
        var deliveredError: WPAPINetworkError? = null
        val request = buildRequest(url = "$SITE/wp-json/wp/v2/users/me?context=edit") { deliveredError = it }

        val result = request.parseNetworkResponse(networkResponse(200, HTML, CHALLENGE_PAGE))
        request.deliverError(result.error)

        assertThat(deliveredError?.type).isEqualTo(GenericErrorType.PARSE_ERROR)
        assertThat(deliveredError?.unexpectedStoreResponse).isEqualTo(
            UnexpectedStoreResponse(
                kind = UnexpectedStoreResponseKind.UNEXPECTED_CONTENT,
                statusCode = 200,
                contentType = HTML,
                requestType = "GET /wp/v2/users/me",
                excerpt = "Just a moment... | Checking your browser."
            )
        )
    }

    @Test
    fun `given a 2xx JSON response, when it is parsed, then it succeeds with the status code`() {
        val request = buildRequest(url = "$SITE/wp-json/wp/v2/users/me") {}

        val result = request.parseNetworkResponse(networkResponse(200, JSON, """{"id":1}"""))

        assertThat(result.isSuccess).isTrue
        assertThat(result.result?.statusCode).isEqualTo(200)
    }

    @Test
    fun `given a 403 firewall page, when the error is delivered, then it adds the details`() {
        val request = buildRequest(
            method = Method.POST,
            url = "$SITE/?rest_route=/wp/v2/users/me/application-passwords"
        ) {}

        val error = request.deliverBaseNetworkError(
            BaseNetworkError(VolleyError(networkResponse(403, HTML, FIREWALL_PAGE)))
        ) as WPAPINetworkError

        assertThat(error.errorCode).isEmpty()
        assertThat(error.unexpectedStoreResponse?.kind).isEqualTo(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE)
        assertThat(error.unexpectedStoreResponse?.requestType)
            .isEqualTo("POST /wp/v2/users/me/application-passwords")
        assertThat(error.unexpectedStoreResponse?.excerpt).isEqualTo("Access Denied | Blocked by the firewall.")
    }

    @Test
    fun `given a JSON error, when the error is delivered, then it keeps the error code and adds no details`() {
        val request = buildRequest(url = "$SITE/wp-json/wc/v3/products") {}

        val error = request.deliverBaseNetworkError(
            BaseNetworkError(
                VolleyError(networkResponse(400, JSON, """{"code":"rest_invalid_param","message":"Invalid."}"""))
            )
        ) as WPAPINetworkError

        assertThat(error.errorCode).isEqualTo("rest_invalid_param")
        assertThat(error.unexpectedStoreResponse).isNull()
    }

    private fun buildRequest(
        method: Int = Method.GET,
        url: String,
        onError: (WPAPINetworkError) -> Unit
    ) = WPAPIGsonRequest(
        method,
        url,
        null,
        null,
        Any::class.java,
        { _: Any? -> },
        { error -> onError(error) }
    )

    private fun networkResponse(statusCode: Int, contentType: String, body: String) =
        NetworkResponse(statusCode, body.toByteArray(), mapOf("Content-Type" to contentType), false)

    private companion object {
        const val SITE = "https://example.com"
        const val HTML = "text/html; charset=UTF-8"
        const val JSON = "application/json; charset=UTF-8"
        const val CHALLENGE_PAGE = "<html><head><title>Just a moment...</title></head>" +
            "<body><p>Checking your browser.</p></body></html>"
        const val FIREWALL_PAGE = "<html><head><title>Access Denied</title></head>" +
            "<body><p>Blocked by the firewall.</p></body></html>"
    }
}
