package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.GsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class WPComGsonRequestTest {
    @Test
    fun testWPComErrorResponse() {
        val url = WPCOMREST.sites.site(123).posts.post(456).urlV1_1
        val request = WPComGsonRequest.buildGetRequest(url, null, Object::class.java,
                mock<GsonRequest.ResponseListener<String>>(), mock())

        val responseJson = "{\"error\":\"unknown_post\",\"message\":\"Unknown post\"}"
        val baseNetworkError = buildErrorResponseObject(responseJson, 404)

        // Simulate a network response for this request
        val augmentedError = request.deliverBaseNetworkError(baseNetworkError) as WPComGsonNetworkError

        assertEquals(GenericErrorType.UNKNOWN, augmentedError.type)
        assertEquals("unknown_post", augmentedError.apiError)
        assertEquals("Unknown post", augmentedError.message)
    }

    @Test
    fun testWPComV2ErrorResponse() {
        val url = WPCOMV2.users.username.suggestions.url
        val request = WPComGsonRequest.buildGetRequest(url, null, Object::class.java,
                mock<GsonRequest.ResponseListener<String>>(), mock())

        val responseJson = "{\"code\":\"rest_no_name\"," +
                "\"message\":\"A name from which to derive username suggestions is required.\"}"
        val baseNetworkError = buildErrorResponseObject(responseJson, 400)

        // Simulate a network response for this request
        val augmentedError = request.deliverBaseNetworkError(baseNetworkError) as WPComGsonNetworkError

        assertEquals(GenericErrorType.UNKNOWN, augmentedError.type)
        assertEquals("rest_no_name", augmentedError.apiError)
        assertEquals("A name from which to derive username suggestions is required.", augmentedError.message)
    }

    @Test
    fun `given top level data raw_body, when WPCom error is parsed, then errorData keeps raw_body`() {
        val url = WPCOMREST.sites.site(123).posts.post(456).urlV1_1
        val request = WPComGsonRequest.buildGetRequest(
            url,
            null,
            Object::class.java,
            mock<GsonRequest.ResponseListener<String>>(),
            mock()
        )

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
        val baseNetworkError = buildErrorResponseObject(responseJson, 502)

        val augmentedError = request.deliverBaseNetworkError(baseNetworkError) as WPComGsonNetworkError

        assertNotNull(augmentedError.errorData)
        assertEquals("<html>Fatal error</html>", augmentedError.errorData?.optString("raw_body"))
    }

    private fun buildErrorResponseObject(responseJson: String, errorCode: Int): BaseNetworkError {
        val networkResponse = NetworkResponse(errorCode, responseJson.toByteArray(), mapOf(), true)
        return BaseNetworkError(VolleyError(networkResponse))
    }
}
