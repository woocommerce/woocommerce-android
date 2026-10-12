package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.Request.Method
import com.android.volley.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.rest.ResponseWithHeaders

@RunWith(RobolectricTestRunner::class)
class WPAPIEncodedBodyRequestTest {
    @Test
    fun `given a 202 page, when the response is delivered, then the listener gets the status code`() {
        var deliveredStatusCode: Int? = null
        val request = WPAPIEncodedBodyRequest(
            method = Method.GET,
            url = "https://example.com/wp-login.php",
            params = emptyMap(),
            body = emptyMap(),
            listener = { _, _, statusCode -> deliveredStatusCode = statusCode },
            errorListener = {}
        )

        val parsed = WPAPIEncodedBodyRequest::class.java
            .getDeclaredMethod("parseNetworkResponse", NetworkResponse::class.java)
            .apply { isAccessible = true }
            .invoke(request, NetworkResponse(202, CHALLENGE_PAGE.toByteArray(), emptyMap(), false))
            as Response<*>
        WPAPIEncodedBodyRequest::class.java
            .getDeclaredMethod("deliverResponse", ResponseWithHeaders::class.java)
            .apply { isAccessible = true }
            .invoke(request, parsed.result)

        assertThat(deliveredStatusCode).isEqualTo(202)
    }

    private companion object {
        const val CHALLENGE_PAGE = "<html><head><meta http-equiv=\"refresh\" content=\"0;/challenge/\"></head></html>"
    }
}
